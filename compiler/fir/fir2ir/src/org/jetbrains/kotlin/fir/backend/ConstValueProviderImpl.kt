/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationCall
import org.jetbrains.kotlin.fir.expressions.builder.buildConstExpression
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.serialization.constant.ConstValueProvider
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.types.ConstantValueKind

class ConstValueProviderImpl(
    private val components: Fir2IrComponents,
) : ConstValueProvider() {
    override val session: FirSession = components.session

    override fun getConstantValueForProperty(firProperty: FirProperty): FirConstExpression<*>? {
        val irProperty: IrProperty = components.declarationStorage.getCachedIrProperty(firProperty) ?: return null
        if (!irProperty.isConst) return null
        val irConst = irProperty.backingField?.initializer?.expression as? IrConst<*> ?: return null
        return irConst.toFirConst()
    }

    override fun getNewFirAnnotationWithConstantValues(
        firAnnotationContainer: FirAnnotationContainer,
        firAnnotation: FirAnnotation,
    ): FirAnnotation {
        if (firAnnotation is FirErrorAnnotationCall) return firAnnotation

        val irDeclaration = when (firAnnotationContainer) {
            is FirClass -> components.classifierStorage.getCachedIrClass(firAnnotationContainer)
            is FirEnumEntry -> components.classifierStorage.getCachedIrEnumEntry(firAnnotationContainer)
            is FirTypeAlias -> components.classifierStorage.getCachedTypeAlias(firAnnotationContainer)
            is FirTypeParameter -> components.classifierStorage.getCachedIrTypeParameter(firAnnotationContainer)

            is FirScript -> components.declarationStorage.getCachedIrScript(firAnnotationContainer)
            is FirConstructor -> components.declarationStorage.getCachedIrConstructor(firAnnotationContainer)
            is FirFunction -> components.declarationStorage.getCachedIrFunction(firAnnotationContainer)
            is FirProperty -> components.declarationStorage.getCachedIrProperty(firAnnotationContainer)
            else -> error("Cannot extract IR declaration for ${firAnnotationContainer.render()}")
        } ?: return firAnnotation

        return buildNewFirAnnotationByCorrespondingIrAnnotation(irDeclaration, firAnnotationContainer, firAnnotation)
    }

    override fun getNewFirAnnotationWithConstantValues(
        firProperty: FirProperty,
        firAnnotation: FirAnnotation,
        firPropertyAccessor: FirPropertyAccessor,
        isGetter: Boolean,
    ): FirAnnotation {
        if (firAnnotation is FirErrorAnnotationCall) return firAnnotation

        val irProperty = components.declarationStorage.getCachedIrProperty(firProperty) ?: return firAnnotation
        val irAccessor = (if (isGetter) irProperty.getter else irProperty.setter)
            ?: error("Cannot extract IR property accessor for ${firProperty.render()}")
        return buildNewFirAnnotationByCorrespondingIrAnnotation(irAccessor, firPropertyAccessor, firAnnotation)
    }

    override fun getNewFirAnnotationWithConstantValues(
        firExtensionReceiverContainer: FirAnnotationContainer,
        firAnnotation: FirAnnotation,
        receiverParameter: FirReceiverParameter
    ): FirAnnotation {
        if (firAnnotation is FirErrorAnnotationCall) return firAnnotation

        val extensionReceiver = when (firExtensionReceiverContainer) {
            is FirFunction -> components.declarationStorage.getCachedIrFunction(firExtensionReceiverContainer)?.extensionReceiverParameter
            is FirProperty -> components.declarationStorage.getCachedIrProperty(firExtensionReceiverContainer)?.getter?.extensionReceiverParameter
            else -> error("Cannot extract IR extension receiver for ${firExtensionReceiverContainer::class}")
        } ?: return firAnnotation

        return buildNewFirAnnotationByCorrespondingIrAnnotation(extensionReceiver, receiverParameter, firAnnotation)
    }

    override fun getNewFirAnnotationWithConstantValues(
        valueParameter: FirValueParameter,
        firAnnotation: FirAnnotation,
    ): FirAnnotation {
        if (firAnnotation is FirErrorAnnotationCall) return firAnnotation

        val firFunction = valueParameter.containingFunctionSymbol.fir
        val irFunction = components.declarationStorage.getCachedIrFunction(firFunction) ?: return firAnnotation
        val irValueParameter = irFunction.valueParameters.single { it.name == valueParameter.name }

        return buildNewFirAnnotationByCorrespondingIrAnnotation(irValueParameter, valueParameter, firAnnotation)
    }

    private fun buildNewFirAnnotationByCorrespondingIrAnnotation(
        irDeclaration: IrDeclarationBase,
        firAnnotationContainer: FirAnnotationContainer,
        firAnnotation: FirAnnotation
    ): FirAnnotation {
        assert(irDeclaration.annotations.size == firAnnotationContainer.annotations.size) {
            "Number of annotations for IR and FIR declaration are not equal"
        }

        val annotationIndex = firAnnotationContainer.annotations.indexOf(firAnnotation)
        val irArguments = irDeclaration.annotations[annotationIndex].getArgumentsWithIr()

        val annotationArgsMapping = buildAnnotationArgumentMapping {
            firAnnotation.argumentMapping.mapping.forEach { (name, firExpression) ->
                if (firExpression is FirConstExpression<*>) {
                    mapping[name] = firExpression
                    return@forEach
                }
                val irExpression = irArguments.single { it.first.name == name }.second
                // TODO recursion for annotations
                mapping[name] = (irExpression as? IrConst<*>)?.toFirConst() ?: firExpression
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