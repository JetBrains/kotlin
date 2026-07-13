/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.mapping

import org.jetbrains.kotlin.backend.common.ir.returnType
import org.jetbrains.kotlin.backend.jvm.InlineClassAbi
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.getCallableReferenceOwnerKClassType
import org.jetbrains.kotlin.backend.jvm.ir.getCallableReferenceTopLevelFlag
import org.jetbrains.kotlin.backend.jvm.ir.isNonReplacedJvmSpecializedGeneric
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.codegen.util.inlinecodegen.ClassInstance
import org.jetbrains.kotlin.codegen.util.inlinecodegen.LightIrType
import org.jetbrains.kotlin.codegen.util.inlinecodegen.SpecTypeParametersUsages
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrStarProjection
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.withNullability
import org.jetbrains.kotlin.ir.util.arguments
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.genericTypeParameterIndex
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.isJvmSpecialized
import org.jetbrains.kotlin.ir.util.isTypeParameter
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.MUTABLE_PROPERTY_REFERENCE_IMPL
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.PROPERTY_REFERENCE_IMPL
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.org.objectweb.asm.Type

data class LightIrTypeMapper(
    private val backendContext: JvmBackendContext,
    private val irTypeToTy: MutableMap<IrType, LightIrType> = mutableMapOf(),
) {
    fun mapType(type: IrType): LightIrType? {
        irTypeToTy[type]?.let { return it }

        val lightIrType = LightIrType(
            type.isMarkedNullable(),
            backendContext.defaultTypeMapper.mapTypeParameter(type, BothSignatureWriter(BothSignatureWriter.Mode.TYPE)).internalName,
            null,
        )

        irTypeToTy[type] = lightIrType

        lightIrType.classifier = when (val classifier = type.classifierOrNull) {
            is IrClassSymbol -> {
                LightIrType.Classifier.Clazz(
                    classifier.owner.fqNameWhenAvailable?.asString() ?: return null,
                    mapInlineAbi(type as IrSimpleType, classifier),
                    backendContext.defaultTypeMapper.generateClassInstance(type, false)
                )
            }
            is IrTypeParameterSymbol -> mapTypeParameter(classifier.owner) ?: return null
            is IrScriptSymbol -> TODO("IrScriptSymbol classifiers are not supported yet")
            null -> TODO("NULL classifiers aro not supported yet")
        }

        lightIrType.arguments = type.arguments.orEmpty().map {
            when (it) {
                is IrStarProjection -> LightIrType.TypeArgument.StarProjection()
                is IrTypeProjection -> LightIrType.TypeArgument.TypeProjection(
                    mapType(it.type) ?: return null,
                    it.variance.toLightIrTreeChar()
                )
            }
        }

        return lightIrType
    }

    private fun mapTypeParameter(typeParameter: IrTypeParameter): LightIrType.Classifier.TypeParameter? {
        return LightIrType.Classifier.TypeParameter(
            name = typeParameter.name.asString(),
            index = typeParameter.index,
            variance = typeParameter.variance.toLightIrTreeChar(),
            isReified = typeParameter.isReified,
            specialized = typeParameter.isJvmSpecialized,
            parent = typeParameter.parent.toLightIrTypeParameterParent(backendContext) ?: return null,
            upperBounds = if (!typeParameter.isReified) typeParameter.superTypes.map { mapType(it) ?: return null } else null,
        )
    }

    private fun mapInlineAbi(simpleType: IrSimpleType, classifier: IrClassSymbol): LightIrType.InlineAbi? {
        val unboxed = InlineClassAbi.unboxType(simpleType.withNullability(false)) ?: return null

        val replacements = mutableListOf<LightIrType.InlineAbi.Replacement>()
        for (replacement in classifier.owner.functions) {
            if (replacement.origin != JvmLoweredDeclarationOrigin.STATIC_INLINE_CLASS_REPLACEMENT) continue
            for (overriddenSymbol in replacement.overriddenSymbols) {
                val overridden = overriddenSymbol.owner
                val replacementForceBoxedReturn = backendContext.defaultMethodSignatureMapper.forceBoxedReturnType(replacement)
                replacements.add(
                    LightIrType.InlineAbi.Replacement(
                        isInterface = overridden.parentAsClass.isInterface,
                        repName = replacement.name.asString(),
                        repDesc = backendContext.defaultMethodSignatureMapper.mapSignatureSkipGeneric(replacement).asmMethod.descriptor,
                        origName = overridden.name.asString(),
                        origDesc = backendContext.defaultMethodSignatureMapper.mapSignatureSkipGeneric(overridden).asmMethod.descriptor,
                        changedParameters = replacement.parameters.zip(overridden.parameters)
                            .filter { [_, orig] -> orig.type.isTypeParameter() }
                            .map { [rep, orig] -> orig.indexInParameters to mapType(rep.type)!! },
                        changedReturnType = if (overridden.returnType.isTypeParameter() && !replacementForceBoxedReturn) mapType(replacement.returnType) else null,
                    )
                )
            }
        }

        return LightIrType.InlineAbi(
            backendContext.defaultTypeMapper.mapType(unboxed).descriptor,
            InlineClassAbi.unboxType(simpleType.withNullability(true)) == null,
            replacements,
        )
    }
}

private fun Variance.toLightIrTreeChar(): Char = when (this) {
    Variance.INVARIANT -> LightIrType.TypeArgument.VARIANCE_INV
    Variance.IN_VARIANCE -> LightIrType.TypeArgument.VARIANCE_IN
    Variance.OUT_VARIANCE -> LightIrType.TypeArgument.VARIANCE_OUT
}

private fun IrDeclarationParent.toLightIrTypeParameterParent(context: JvmBackendContext): LightIrType.Classifier.TypeParameter.Parent? {
    return when (this) {
        is IrClass -> {
            val classInstance = context.defaultTypeMapper.generateClassInstance(defaultType, true) as ClassInstance.ConstClass
            LightIrType.Classifier.TypeParameter.Parent.ParentClass(Type.getType(classInstance.descriptor).internalName)
        }
        is IrSimpleFunction -> {
            val property = correspondingPropertySymbol
            if (property != null) {
                val property = property.owner
                val getter = property.getter ?: error("Property without getter: ${property.render()}")
                val arity = getter.parameters.size
                val implClass = (if (property.isVar) MUTABLE_PROPERTY_REFERENCE_IMPL else PROPERTY_REFERENCE_IMPL).getOrNull(arity)
                    ?: error("No property reference impl class with arity $arity (${property.render()}")
                LightIrType.Classifier.TypeParameter.Parent.Property(
                    implClass.internalName,
                    context.defaultTypeMapper.generateClassInstance(getCallableReferenceOwnerKClassType(context), false),
                    property.name.asString(),
                    context.defaultMethodSignatureMapper.generateSignatureString(getter),
                    getCallableReferenceTopLevelFlag(),
                )
            } else {
                LightIrType.Classifier.TypeParameter.Parent.Function(
                    parameters.size,
                    context.defaultTypeMapper.generateClassInstance(getCallableReferenceOwnerKClassType(context), false),
                    name.asString(),
                    context.defaultMethodSignatureMapper.generateSignatureString(this),
                    getCallableReferenceTopLevelFlag(),
                )
            }
        }
        else -> error("parent is not IrClass or IrSimpleFunction")
    }
}

fun IrType.asSpecTypeParameterUsage(): SpecTypeParametersUsages.Usage? =
    if (isNonReplacedJvmSpecializedGeneric) SpecTypeParametersUsages.Usage(genericTypeParameterIndex!!, isMarkedNullable()) else null

fun IrFunction.specTypeParametersUsages(): SpecTypeParametersUsages {
    return SpecTypeParametersUsages(
        buildMap {
            for ([parameterIndex, parameter] in parameters.withIndex()) {
                parameter.type.asSpecTypeParameterUsage()?.let {
                    put(parameterIndex, it)
                }
            }
        },
        returnType.asSpecTypeParameterUsage(),
    )
}
