/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.backend.common.extensions.IrGeneratedDeclarationsRegistrar
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildExpressionStub
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.getContainingFile
import org.jetbrains.kotlin.fir.resolve.providers.toSymbol
import org.jetbrains.kotlin.fir.resolve.toFirRegularClass
import org.jetbrains.kotlin.fir.serialization.FirAdditionalMetadataProvider
import org.jetbrains.kotlin.fir.serialization.providedDeclarationsForMetadataService
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.utils.addToStdlib.runIf

// opt-in is safe, this code runs after fir2ir is over and all symbols are bound
@OptIn(UnsafeDuringIrConstructionAPI::class)
class Fir2IrIrGeneratedDeclarationsRegistrar(private val components: Fir2IrComponents) : IrGeneratedDeclarationsRegistrar() {
    private val session: FirSession
        get() = components.session

    private val implicitType: FirImplicitTypeRef
        get() = FirImplicitTypeRefImplWithoutSource

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

    override fun registerFunctionAsMetadataVisible(irFunction: IrSimpleFunction) {
        if (irFunction.isLocal || irFunction.parentClassOrNull?.isLocal == true) return
        val firFunction = buildSimpleFunction {
            moduleData = session.moduleData
            origin = GeneratedForMetadata.origin
            status = FirResolvedDeclarationStatusImpl(
                irFunction.visibility.delegate,
                irFunction.modality,
                irFunction.visibility.delegate.toEffectiveVisibility(owner = null)
            ).apply {
                isExpect = irFunction.isExpect
                isActual = false
                isOverride = irFunction.overriddenSymbols.isNotEmpty()
                isInfix = irFunction.isInfix
                isInline = irFunction.isInline
                isTailRec = irFunction.isTailrec
                isSuspend = irFunction.isSuspend
            }
            returnTypeRef = implicitType
            dispatchReceiverType = irFunction.parent.toFirClass()?.defaultType()
            // contextReceivers
            // valueParameters
            name = irFunction.name
            symbol = FirNamedFunctionSymbol(irFunction.callableId)
            // annotations
            irFunction.typeParameters.mapTo(typeParameters) {
                buildTypeParameter {
                    moduleData = session.moduleData
                    origin = GeneratedForMetadata.origin
                    name = it.name
                    symbol = FirTypeParameterSymbol()
                    containingDeclarationSymbol = this@buildSimpleFunction.symbol
                    variance = it.variance
                    isReified = it.isReified
                    // bounds
                    // annotations
                }
            }
        }

        with(TypeConverter(irFunction, firFunction)) {
            with(firFunction) {
                replaceReturnTypeRef(irFunction.returnType.toConeType().toFirResolvedTypeRef())
                val valueParameters = irFunction.valueParameters.map {
                    buildValueParameter {
                        moduleData = session.moduleData
                        origin = GeneratedForMetadata.origin
                        returnTypeRef = it.type.toConeType().toFirResolvedTypeRef()
                        name = it.name
                        symbol = FirValueParameterSymbol(name)
                        if (it.defaultValue != null) {
                            defaultValue = buildExpressionStub {
                                coneTypeOrNull = this@buildValueParameter.returnTypeRef.coneType
                            }
                        }
                        containingFunctionSymbol = firFunction.symbol
                        isCrossinline = it.isCrossinline
                        isNoinline = it.isNoinline
                        isVararg = it.isVararg
                        annotations.addAll(it.convertAnnotations())
                    }
                }
                replaceValueParameters(valueParameters)

                for ((firParameter, irParameter) in typeParameters.zip(irFunction.typeParameters)) {
                    val newBounds = irParameter.superTypes.map { it.toConeType().toFirResolvedTypeRef() }
                    firParameter.replaceBounds(newBounds)
                    firParameter.replaceAnnotations(irParameter.convertAnnotations())
                }

                replaceAnnotations(irFunction.convertAnnotations())
            }
        }

        session.providedDeclarationsForMetadataService.registerDeclaration(firFunction)

        irFunction.metadata = FirMetadataSource.Function(firFunction)
    }

    override fun registerConstructorAsMetadataVisible(irConstructor: IrConstructor) {
        if (irConstructor.isLocal || irConstructor.parentAsClass.isLocal) return
        val constructedClass = irConstructor.parent.toFirClass()
            ?: error("Fir class for constructor ${irConstructor.render()} not found")
        val firConstructor = buildConstructor {
            moduleData = session.moduleData
            origin = GeneratedForMetadata.origin
            status = FirResolvedDeclarationStatusImpl(
                irConstructor.visibility.delegate,
                Modality.FINAL,
                irConstructor.visibility.delegate.toEffectiveVisibility(owner = null)
            ).apply {
                isExpect = irConstructor.isExpect
                isActual = false
            }
            returnTypeRef = implicitType

            // contextReceivers
            // valueParameters
            symbol = FirConstructorSymbol(constructedClass.classId)
            // annotations
            constructedClass.typeParameters.mapTo(typeParameters) { buildConstructedClassTypeParameterRef { symbol = it.symbol } }
        }

        with(TypeConverter(irConstructor, firConstructor)) {
            with(firConstructor) {
                replaceReturnTypeRef(irConstructor.returnType.toConeType().toFirResolvedTypeRef())
                val valueParameters = irConstructor.valueParameters.map {
                    buildValueParameter {
                        moduleData = session.moduleData
                        origin = GeneratedForMetadata.origin
                        returnTypeRef = it.type.toConeType().toFirResolvedTypeRef()
                        name = it.name
                        symbol = FirValueParameterSymbol(name)
                        if (it.defaultValue != null) {
                            defaultValue = buildExpressionStub {
                                coneTypeOrNull = this@buildValueParameter.returnTypeRef.coneType
                            }
                        }
                        containingFunctionSymbol = firConstructor.symbol
                        isCrossinline = it.isCrossinline
                        isNoinline = it.isNoinline
                        isVararg = it.isVararg
                        annotations.addAll(it.convertAnnotations())
                    }
                }
                replaceValueParameters(valueParameters)
                replaceAnnotations(irConstructor.convertAnnotations())
                containingClassForStaticMemberAttr = constructedClass.symbol.toLookupTag()
            }
        }

        session.providedDeclarationsForMetadataService.registerDeclaration(firConstructor)

        irConstructor.metadata = FirMetadataSource.Function(firConstructor)
    }

    fun createAdditionalMetadataProvider(): FirAdditionalMetadataProvider {
        return Provider()
    }

    private fun IrDeclarationParent.toFirClass(): FirRegularClass? {
        return (this as? IrClass)?.classIdOrFail?.toLookupTag()?.toFirRegularClass(session)
    }

    private fun IrAnnotationContainer.convertAnnotations(): List<FirAnnotation> {
        return this.annotations.map { it.toFirAnnotation() }
    }

    private inner class TypeConverter(val originalFunction: IrFunction, val convertedFunction: FirFunction) {
        fun IrType.toConeType(): ConeKotlinType {
            return when (this) {
                is IrSimpleType -> {
                    val lookupTag = classifier.toLookupTag()
                    lookupTag.constructType(
                        this.arguments.map { it.toConeTypeProjection() }.toTypedArray(),
                        isNullable = this.isNullable()
                    )
                }
                is IrDynamicType -> ConeDynamicType.create(session)
                else -> error("Unsupported IR type: $this")
            }
        }

        private fun IrTypeArgument.toConeTypeProjection(): ConeTypeProjection {
            return when (this) {
                is IrStarProjection -> ConeStarProjection
                is IrTypeProjection -> type.toConeType().toTypeProjection(variance)
            }
        }

        private fun IrClassifierSymbol.toLookupTag(): ConeClassifierLookupTag {
            return when (val owner = owner) {
                is IrClass -> owner.classIdOrFail.toLookupTag()
                is IrTypeParameter -> {
                    val typeParameter = when (val parent = owner.parent) {
                        originalFunction -> convertedFunction.typeParameters[owner.index]
                        is IrClass -> {
                            val firClass = parent.classIdOrFail.toLookupTag().toFirRegularClass(session)
                                ?: error("Fir class for ${parent.render()} not found")
                            firClass.typeParameters[owner.index]
                        }
                        else -> error("Unsupported type parameter container: ${parent.render()}")
                    }
                    typeParameter.symbol.toLookupTag()
                }
                else -> error("Unsupported IR classifier: ${owner.render()}")
            }
        }
    }

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
                        IrConstKind.Boolean -> buildLiteralExpression(
                            source = null,
                            ConstantValueKind.Boolean,
                            argument.value as Boolean,
                            setType = true
                        )
                        IrConstKind.Byte -> buildLiteralExpression(
                            source = null,
                            ConstantValueKind.Byte,
                            argument.value as Byte,
                            setType = true
                        )
                        IrConstKind.Char -> buildLiteralExpression(
                            source = null,
                            ConstantValueKind.Char,
                            argument.value as Char,
                            setType = true
                        )
                        IrConstKind.Double -> buildLiteralExpression(
                            source = null,
                            ConstantValueKind.Double,
                            argument.value as Double,
                            setType = true
                        )
                        IrConstKind.Float -> buildLiteralExpression(
                            source = null,
                            ConstantValueKind.Float,
                            argument.value as Float,
                            setType = true
                        )
                        IrConstKind.Int -> buildLiteralExpression(
                            source = null,
                            ConstantValueKind.Int,
                            argument.value as Int,
                            setType = true
                        )
                        IrConstKind.Long -> buildLiteralExpression(
                            source = null,
                            ConstantValueKind.Long,
                            argument.value as Long,
                            setType = true
                        )
                        IrConstKind.Null -> buildLiteralExpression(
                            source = null,
                            ConstantValueKind.Null,
                            value = null,
                            setType = true
                        )
                        IrConstKind.Short -> buildLiteralExpression(
                            source = null,
                            ConstantValueKind.Short,
                            argument.value as Short,
                            setType = false
                        )
                        IrConstKind.String -> buildLiteralExpression(
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

    private object GeneratedForMetadata : GeneratedDeclarationKey()

    private inner class Provider : FirAdditionalMetadataProvider() {
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
            return fileStorage[(source.startOffsetSkippingComments() ?: source.startOffset) to source.endOffset] ?: emptyList()
        }

        private fun FirDeclaration.containingFile(): FirFile? {
            if (this is FirFile) return this
            // In MPP scenario containing session of declaration may differ from the main session of the module
            //  (if this declaration came from some common module), so in order to get the proper containing file,
            //  we need to use the original session of the declaration
            val containingSession = moduleData.session
            val topmostParent = topmostParent(containingSession)
            return components.firProvider.getContainingFile(topmostParent.symbol)
        }

        private fun FirDeclaration.topmostParent(session: FirSession): FirDeclaration {
            return when (this) {
                is FirClassLikeDeclaration -> runIf(!classId.isLocal) { classId.topmostParentClassId.toSymbol(session)?.fir }
                is FirTypeParameter -> containingDeclarationSymbol.fir.topmostParent(session)
                is FirValueParameter -> containingFunctionSymbol.fir.topmostParent(session)
                is FirCallableDeclaration -> symbol.callableId.classId
                    ?.takeIf { !it.isLocal }
                    ?.topmostParentClassId
                    ?.toSymbol(session)
                    ?.fir
                is FirScript -> this
                else -> error("Unsupported declaration type: $this")
            } ?: this
        }

        private val ClassId.topmostParentClassId: ClassId
            get() = parentClassId?.topmostParentClassId ?: this
    }
}
