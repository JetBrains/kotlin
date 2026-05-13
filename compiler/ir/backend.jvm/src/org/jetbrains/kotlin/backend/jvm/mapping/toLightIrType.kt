/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.mapping

import org.jetbrains.kotlin.backend.jvm.InlineClassAbi
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.getCallableReferenceOwnerKClassType
import org.jetbrains.kotlin.backend.jvm.ir.getCallableReferenceTopLevelFlag
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
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.withNullability
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.genericTypeParameterIndex
import org.jetbrains.kotlin.ir.util.isJvmSpecialized
import org.jetbrains.kotlin.ir.util.isJvmSpecializedGeneric
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.MUTABLE_PROPERTY_REFERENCE_IMPL
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.PROPERTY_REFERENCE_IMPL
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.org.objectweb.asm.Type

fun IrType.toLightIrType(context: JvmBackendContext, visitedTypeParameters: MutableSet<IrTypeParameter> = HashSet()): LightIrType? {
    val simpleType = this as? IrSimpleType ?: return null
    val sw = BothSignatureWriter(BothSignatureWriter.Mode.TYPE)

    val classifier = when (val classifier = simpleType.classifier) {
        is IrClassSymbol -> {
            LightIrType.Classifier.Clazz(
                classifier.owner.fqNameWhenAvailable?.asString() ?: return null,
                InlineClassAbi.unboxType(simpleType.withNullability(false))?.let { unboxed ->
                    LightIrType.InlineAbi(
                        context.defaultTypeMapper.mapType(unboxed).descriptor,
                        InlineClassAbi.unboxType(simpleType.withNullability(true)) == null,
                    )
                },
                context.defaultTypeMapper.generateClassInstance(this, false)
            )
        }
        is IrTypeParameterSymbol -> classifier.owner.toLightIrTypeParameter(context, visitedTypeParameters) ?: return null
        is IrScriptSymbol -> TODO("IrScriptSymbol classifiers are not supported yet")
    }

    val arguments = simpleType.arguments.map {
        when (it) {
            is IrStarProjection -> LightIrType.TypeArgument.StarProjection()
            is IrTypeProjection -> LightIrType.TypeArgument.TypeProjection(
                it.type.toLightIrType(context, visitedTypeParameters) ?: return null,
                it.variance.toLightIrTreeChar()
            )
        }
    }

    return LightIrType(
        classifier,
        arguments,
        isMarkedNullable(),
        context.defaultTypeMapper.mapTypeParameter(simpleType, sw).internalName,
    )
}

private fun Variance.toLightIrTreeChar(): Char = when (this) {
    Variance.INVARIANT -> LightIrType.TypeArgument.VARIANCE_INV
    Variance.IN_VARIANCE -> LightIrType.TypeArgument.VARIANCE_IN
    Variance.OUT_VARIANCE -> LightIrType.TypeArgument.VARIANCE_OUT
}

private fun IrTypeParameter.toLightIrTypeParameter(
    context: JvmBackendContext,
    visitedTypeParameters: MutableSet<IrTypeParameter>,
): LightIrType.Classifier.TypeParameter? {
    if (!visitedTypeParameters.add(this)) error("recursive non-reified type parameter bounds not supported (TODO: report proper diagnostic)")
    return LightIrType.Classifier.TypeParameter(
        name.asString(),
        index,
        variance.toLightIrTreeChar(),
        isReified,
        isJvmSpecialized,
        parent.toLightIrTypeParameterParent(context) ?: return null,
        if (!isReified) superTypes.map { it.toLightIrType(context, visitedTypeParameters) ?: return null } else null,
    ).also {
        visitedTypeParameters.remove(this)
    }
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
    if (isJvmSpecializedGeneric) SpecTypeParametersUsages.Usage(genericTypeParameterIndex!!, isMarkedNullable()) else null

fun IrFunction.specTypeParametersUsages(): SpecTypeParametersUsages {
    return SpecTypeParametersUsages(
        buildMap {
            for ((parameterIndex, parameter) in parameters.withIndex()) {
                parameter.type.asSpecTypeParameterUsage()?.let {
                    put(parameterIndex, it)
                }
            }
        },
        returnType.asSpecTypeParameterUsage(),
    )
}
