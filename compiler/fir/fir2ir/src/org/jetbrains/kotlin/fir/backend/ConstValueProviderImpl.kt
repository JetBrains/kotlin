/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.serialization.constant.ConstValueProvider
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.types.ConstantValueKind

class ConstValueProviderImpl(
    private val components: Fir2IrComponents,
) : ConstValueProvider() {
    override val session: FirSession = components.session

    override fun getConstantValueForProperty(firProperty: FirProperty): FirExpression? {
        val firInitializer = firProperty.initializer
        val irProperty: IrProperty = components.declarationStorage.getCachedIrProperty(firProperty) ?: return firInitializer
        val irConst = irProperty.backingField?.initializer?.expression as? IrConst<*> ?: return firInitializer
        return irConst.toFirConst() ?: firInitializer
    }

    override fun getNewFirAnnotationWithConstantValues(
        firAnnotationContainer: FirAnnotationContainer,
        firAnnotation: FirAnnotation,
    ): FirAnnotation {
        if (firAnnotation is FirErrorAnnotationCall || firAnnotation.source == null) return firAnnotation

        val irDeclaration = when (firAnnotationContainer) {
            is FirClass -> components.classifierStorage.getCachedIrClass(firAnnotationContainer)
            is FirEnumEntry -> components.classifierStorage.getCachedIrEnumEntry(firAnnotationContainer)
            is FirTypeAlias -> components.classifierStorage.getCachedTypeAlias(firAnnotationContainer)
            is FirTypeParameter -> components.classifierStorage.getCachedIrTypeParameter(firAnnotationContainer)

            is FirScript -> components.declarationStorage.getCachedIrScript(firAnnotationContainer)
            is FirConstructor -> components.declarationStorage.getCachedIrConstructor(firAnnotationContainer)
            is FirPropertyAccessor -> components.declarationStorage.getCachedIrProperty(firAnnotationContainer.propertySymbol.fir)
                ?.let { if (firAnnotationContainer.isGetter) it.getter else it.setter }
            is FirFunction -> components.declarationStorage.getCachedIrFunction(firAnnotationContainer)
            is FirProperty -> components.declarationStorage.getCachedIrProperty(firAnnotationContainer)
            is FirValueParameter -> components.declarationStorage.getCachedIrFunction(firAnnotationContainer.containingFunctionSymbol.fir)
                ?.valueParameters?.single { it.name == firAnnotationContainer.name }
            else -> error("Cannot extract IR declaration for ${firAnnotationContainer.render()}")
        } ?: return firAnnotation

        val correctFirContainer = when (firAnnotation.useSiteTarget) {
            AnnotationUseSiteTarget.FIELD, AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD -> {
                (firAnnotationContainer as? FirProperty)?.backingField ?: return firAnnotation
            }
            else -> firAnnotationContainer
        }

        val correctIrDeclaration = when (firAnnotation.useSiteTarget) {
            AnnotationUseSiteTarget.FIELD, AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD -> {
                (irDeclaration as? IrProperty)?.backingField ?: return firAnnotation
            }
            else -> irDeclaration
        }

        return buildNewFirAnnotationByCorrespondingIrAnnotation(correctIrDeclaration, correctFirContainer, firAnnotation)
    }

    override fun getNewFirAnnotationWithConstantValues(
        firExtensionReceiverContainer: FirAnnotationContainer,
        firAnnotation: FirAnnotation,
        receiverParameter: FirReceiverParameter
    ): FirAnnotation {
        if (firAnnotation is FirErrorAnnotationCall || firAnnotation.source == null) return firAnnotation

        val extensionReceiver = when (firExtensionReceiverContainer) {
            is FirFunction -> components.declarationStorage.getCachedIrFunction(firExtensionReceiverContainer)?.extensionReceiverParameter
            is FirProperty -> components.declarationStorage.getCachedIrProperty(firExtensionReceiverContainer)?.getter?.extensionReceiverParameter
            else -> error("Cannot extract IR extension receiver for ${firExtensionReceiverContainer::class}")
        } ?: return firAnnotation

        return buildNewFirAnnotationByCorrespondingIrAnnotation(extensionReceiver, receiverParameter, firAnnotation)
    }

    private fun buildNewFirAnnotationByCorrespondingIrAnnotation(
        irDeclaration: IrDeclarationBase,
        firAnnotationContainer: FirAnnotationContainer,
        firAnnotation: FirAnnotation
    ): FirAnnotation {
        val unwrappedIrAnnotations = irDeclaration.annotations.flatMap {
            val containerClass = it.symbol.owner.parentAsClass
            val realAnnotation = containerClass.parentClassOrNull ?: return@flatMap listOf(it)
            if (realAnnotation.kind != ClassKind.ANNOTATION_CLASS || !realAnnotation.hasAnnotation(StandardNames.FqNames.repeatable)) {
                return@flatMap listOf(it)
            }

            (it.getValueArgument(0) as IrVararg).elements.map { repAnno -> repAnno as IrConstructorCall }
        }.filter { it.startOffset != -1 && it.endOffset != -1 }

        assert(unwrappedIrAnnotations.size == firAnnotationContainer.annotations.size) {
            "Number of annotations for IR and FIR declaration are not equal"
        }

        val irArguments = unwrappedIrAnnotations
            .find { it.startOffset == firAnnotation.source?.startOffset && it.endOffset == firAnnotation.source?.endOffset }
            ?.getArgumentsWithIr()
            ?: error("Could not find corresponding IR annotation for ${firAnnotation.render()}")

        val annotationArgsMapping = buildAnnotationArgumentMapping {
            firAnnotation.argumentMapping.mapping.forEach { (name, firExpression) ->
                if (firExpression is FirConstExpression<*>) {
                    mapping[name] = firExpression
                    return@forEach
                }
                val irExpression = irArguments.single { it.first.name == name }.second
                mapping[name] = irExpression.convertToFir(firExpression)
            }
        }

        return when (firAnnotation) {
            is FirAnnotationCall -> buildAnnotationCall {
                this.annotationTypeRef = firAnnotation.annotationTypeRef
                this.calleeReference = firAnnotation.calleeReference
                this.argumentMapping = annotationArgsMapping
            }
            else -> buildAnnotation {
                this.annotationTypeRef = firAnnotation.annotationTypeRef
                this.argumentMapping = annotationArgsMapping
            }
        }
    }

    private fun IrElement.convertToFir(firExpression: FirExpression): FirExpression {
        return when {
            this is IrConst<*> -> toFirConst() ?: firExpression
            this is IrConstructorCall && firExpression is FirFunctionCall -> {
                val resolvedArgumentMapping = firExpression.resolvedArgumentMapping ?: return firExpression
                val irArgs = this.getArgumentsWithIr()
                buildFunctionCall {
                    this.source = firExpression.source
                    this.typeRef = firExpression.typeRef
                    this.dispatchReceiver = firExpression.dispatchReceiver
                    this.extensionReceiver = firExpression.extensionReceiver
                    this.explicitReceiver = firExpression.explicitReceiver
                    this.calleeReference = firExpression.calleeReference
                    this.annotations.addAll(firExpression.annotations)
                    this.typeArguments.addAll(firExpression.typeArguments)

                    val newArgMapping = LinkedHashMap(
                        resolvedArgumentMapping.map { (key, value) ->
                            irArgs.first { it.first.name == value.name }.second.convertToFir(key) to value
                        }.toMap()
                    )
                    this.argumentList = buildResolvedArgumentList(newArgMapping, firExpression.argumentList.source)
                }
            }
            this is IrVararg && firExpression is FirVarargArgumentsExpression -> buildVarargArgumentsExpression {
                this.source = firExpression.source
                this.typeRef = firExpression.typeRef
                this.annotations.addAll(firExpression.annotations)
                this.varargElementType = firExpression.varargElementType
                this.arguments.addAll(
                    firExpression.arguments.zip(this@convertToFir.elements).map { it.second.convertToFir(it.first) }
                )
            }
            this is IrVararg && firExpression is FirArrayOfCall -> buildArrayOfCall {
                this.source = firExpression.source
                this.typeRef = firExpression.typeRef
                this.annotations.addAll(firExpression.annotations)
                this.argumentList = buildArgumentList {
                    this.source = firExpression.argumentList.source
                    this.arguments.addAll(
                        firExpression.argumentList.arguments.zip(this@convertToFir.elements).map { it.second.convertToFir(it.first) }
                    )
                }
            }
            else -> firExpression
        }
    }

    private fun IrConst<*>.getConstantKind(): ConstantValueKind<*>? {
        if (this.kind == IrConstKind.Null) return ConstantValueKind.Null

        val constType = this.type.makeNotNull().removeAnnotations()
        return when (this.type.getPrimitiveType()) {
            PrimitiveType.BOOLEAN -> ConstantValueKind.Boolean
            PrimitiveType.CHAR -> ConstantValueKind.Char
            PrimitiveType.BYTE -> ConstantValueKind.Byte
            PrimitiveType.SHORT -> ConstantValueKind.Short
            PrimitiveType.INT -> ConstantValueKind.Int
            PrimitiveType.LONG -> ConstantValueKind.Long
            PrimitiveType.FLOAT -> ConstantValueKind.Float
            PrimitiveType.DOUBLE -> ConstantValueKind.Double
            null -> when (constType.getUnsignedType()) {
                UnsignedType.UBYTE -> ConstantValueKind.UnsignedByte
                UnsignedType.USHORT -> ConstantValueKind.UnsignedShort
                UnsignedType.UINT -> ConstantValueKind.UnsignedInt
                UnsignedType.ULONG -> ConstantValueKind.UnsignedLong
                null -> when {
                    constType.isString() -> ConstantValueKind.String
                    else -> null
                }
            }
        }
    }

    private fun <T> IrConst<T>.toFirConst(): FirConstExpression<T>? {
        @Suppress("UNCHECKED_CAST")
        val kind = getConstantKind() as? ConstantValueKind<T> ?: return null
        return buildConstExpression(null, kind, this.value)
    }
}