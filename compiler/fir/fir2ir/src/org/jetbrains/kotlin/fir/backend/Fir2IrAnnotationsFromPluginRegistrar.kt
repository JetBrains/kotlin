/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.backend.common.extensions.IrAnnotationsFromPluginRegistrar
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildConstExpression
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.getContainingFile
import org.jetbrains.kotlin.fir.resolve.providers.toSymbol
import org.jetbrains.kotlin.fir.serialization.FirAdditionalMetadataAnnotationsProvider
import org.jetbrains.kotlin.fir.types.constructClassType
import org.jetbrains.kotlin.fir.types.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.nameWithPackage
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrSymbolInternals
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.utils.addToStdlib.runIf

// opt-in is safe, this code runs after fir2ir is over and all symbols are bound
@OptIn(IrSymbolInternals::class)
class Fir2IrAnnotationsFromPluginRegistrar(private val components: Fir2IrComponents) : IrAnnotationsFromPluginRegistrar() {
    private val generatedIrDeclarationsByFileByOffset = mutableMapOf<String, MutableMap<Pair<Int, Int>, MutableList<IrConstructorCall>>>()

    private fun IrConstructorCall.hasOnlySupportedAnnotationArgumentTypes(): Boolean {
        for (i in 0 until valueArgumentsCount) {
            if (getValueArgument(i) !is IrConst<*>) {
                return false
            }
        }
        return true
    }

    override fun addMetadataVisibleAnnotationsToElement(declaration: IrDeclaration, annotations: List<IrConstructorCall>) {
        require(annotations.all { it.typeArgumentsCount == 0 && it.hasOnlySupportedAnnotationArgumentTypes() }) {
            "Saving annotations with arguments from IR to metadata is only supported for basic constants. See KT-58968"
        }
        annotations.forEach {
            require(it.symbol.owner.constructedClass.isAnnotationClass) { "${it.render()} is not an annotation constructor call" }
        }
        val fileFqName = declaration.file.nameWithPackage
        val fileStorage = generatedIrDeclarationsByFileByOffset.getOrPut(fileFqName) { mutableMapOf() }
        val storage = fileStorage.getOrPut(declaration.startOffset to declaration.endOffset) { mutableListOf() }
        storage += annotations
        declaration.annotations += annotations
    }

    fun createMetadataAnnotationsProvider(): FirAdditionalMetadataAnnotationsProvider {
        return Provider()
    }

    private inner class Provider : FirAdditionalMetadataAnnotationsProvider() {
        private val session = components.session

        override fun findGeneratedAnnotationsFor(declaration: FirDeclaration): List<FirAnnotation> {
            val irAnnotations = extractGeneratedIrDeclarations(declaration).takeUnless { it.isEmpty() } ?: return emptyList()
            return irAnnotations.map { it.toFirAnnotation() }
        }

        override fun hasGeneratedAnnotationsFor(declaration: FirDeclaration): Boolean {
            return extractGeneratedIrDeclarations(declaration).isNotEmpty()
        }

        private fun extractGeneratedIrDeclarations(declaration: FirDeclaration): List<IrConstructorCall> {
            val firFile = declaration.containingFile() ?: return emptyList()
            val fileFqName = firFile.packageFqName.child(Name.identifier(firFile.name)).asString()
            val source = declaration.source ?: return emptyList()
            val fileStorage = generatedIrDeclarationsByFileByOffset[fileFqName] ?: return emptyList()
            return fileStorage[source.startOffset to source.endOffset] ?: emptyList()
        }

        private fun FirDeclaration.containingFile(): FirFile? {
            if (this is FirFile) return this
            val topmostParent = topmostParent()
            return components.session.firProvider.getContainingFile(topmostParent.symbol)
        }

        private fun FirDeclaration.topmostParent(): FirDeclaration {
            return when (this) {
                is FirClassLikeDeclaration -> runIf(!classId.isLocal) { classId.topmostParentClassId.toSymbol(session)?.fir }
                is FirTypeParameter -> containingDeclarationSymbol.fir.topmostParent()
                is FirValueParameter -> containingFunctionSymbol.fir.topmostParent()
                is FirCallableDeclaration -> symbol.callableId.classId
                    ?.takeIf { !it.isLocal }
                    ?.topmostParentClassId
                    ?.toSymbol(session)
                    ?.fir
                else -> error("Unsupported declaration type: $this")
            } ?: this
        }

        private val ClassId.topmostParentClassId: ClassId
            get() = parentClassId?.topmostParentClassId ?: this

        private fun IrConstructorCall.toFirAnnotation(): FirAnnotation {
            val annotationClassId = this.symbol.owner.constructedClass.classId!!
            return buildAnnotation {
                annotationTypeRef = annotationClassId
                    .toLookupTag()
                    .constructClassType(typeArguments = emptyArray(), isNullable = false)
                    .toFirResolvedTypeRef()
                argumentMapping = buildAnnotationArgumentMapping {
                    for (i in 0 until this@toFirAnnotation.valueArgumentsCount) {
                        val name = this@toFirAnnotation.symbol.owner.valueParameters[i].name
                        val argument = this@toFirAnnotation.getValueArgument(i) as IrConst<*>
                        this.mapping[name] = when (argument.kind) {
                            IrConstKind.Boolean -> buildConstExpression(
                                source = null,
                                ConstantValueKind.Boolean,
                                argument.value as Boolean,
                                setType = true
                            )
                            IrConstKind.Byte -> buildConstExpression(
                                source = null,
                                ConstantValueKind.Byte,
                                argument.value as Byte,
                                setType = true
                            )
                            IrConstKind.Char -> buildConstExpression(
                                source = null,
                                ConstantValueKind.Char,
                                argument.value as Char,
                                setType = true
                            )
                            IrConstKind.Double -> buildConstExpression(
                                source = null,
                                ConstantValueKind.Double,
                                argument.value as Double,
                                setType = true
                            )
                            IrConstKind.Float -> buildConstExpression(
                                source = null,
                                ConstantValueKind.Float,
                                argument.value as Float,
                                setType = true
                            )
                            IrConstKind.Int -> buildConstExpression(
                                source = null,
                                ConstantValueKind.Int,
                                argument.value as Int,
                                setType = true
                            )
                            IrConstKind.Long -> buildConstExpression(
                                source = null,
                                ConstantValueKind.Long,
                                argument.value as Long,
                                setType = true
                            )
                            IrConstKind.Null -> buildConstExpression(
                                source = null,
                                ConstantValueKind.Null,
                                value = null,
                                setType = true
                            )
                            IrConstKind.Short -> buildConstExpression(
                                source = null,
                                ConstantValueKind.Short,
                                argument.value as Short,
                                setType = false
                            )
                            IrConstKind.String -> buildConstExpression(
                                source = null,
                                ConstantValueKind.String,
                                argument.value as String,
                                setType = false
                            )
                        }
                    }
                }
            }
        }
    }
}
