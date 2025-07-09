/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import jdk.nashorn.internal.objects.NativeFunction.function
import org.jetbrains.kotlin.backend.common.serialization.encodings.ClassFlags
import org.jetbrains.kotlin.backend.common.serialization.encodings.FieldFlags
import org.jetbrains.kotlin.backend.common.serialization.encodings.FunctionFlags
import org.jetbrains.kotlin.backend.common.serialization.encodings.LocalVariableFlags
import org.jetbrains.kotlin.backend.common.serialization.encodings.PropertyFlags
import org.jetbrains.kotlin.backend.common.serialization.encodings.RichFunctionReferenceFlags
import org.jetbrains.kotlin.backend.common.serialization.encodings.RichFunctionReferenceFlags.Companion.HAS_SUSPEND_CONVERSION
import org.jetbrains.kotlin.backend.common.serialization.encodings.RichFunctionReferenceFlags.Companion.HAS_UNIT_CONVERSION
import org.jetbrains.kotlin.backend.common.serialization.encodings.RichFunctionReferenceFlags.Companion.HAS_VARARG_CONVERSION
import org.jetbrains.kotlin.backend.common.serialization.encodings.RichFunctionReferenceFlags.Companion.IS_RESTRICTED_SUSPENSION
import org.jetbrains.kotlin.backend.common.serialization.encodings.TypeAliasFlags
import org.jetbrains.kotlin.backend.common.serialization.encodings.TypeParameterFlags
import org.jetbrains.kotlin.backend.common.serialization.encodings.ValueParameterFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.ProtoEnumFlags
import org.jetbrains.kotlin.serialization.deserialization.descriptorVisibility

fun ClassFlags.Companion.encode(clazz: IrClass, languageVersionSettings: LanguageVersionSettings): Long {
    return clazz.run {
        val hasAnnotation = annotations.isNotEmpty()
        val visibility = ProtoEnumFlags.descriptorVisibility(visibility.normalize())
        val modality = ProtoEnumFlags.modality(modality)
        val kind = ProtoEnumFlags.classKind(kind, isCompanion)

        val hasEnumEntries = kind == ProtoBuf.Class.Kind.ENUM_CLASS &&
                languageVersionSettings.supportsFeature(LanguageFeature.EnumEntries)
        val flags = IrFlags.getClassFlags(
            hasAnnotation, visibility, modality, kind, isInner, isData, isExternal, isExpect, isValue, isFun, hasEnumEntries
        )

        flags.toLong()
    }
}

fun FunctionFlags.Companion.encode(function: IrSimpleFunction): Long {
    function.run {
        val hasAnnotation = annotations.isNotEmpty()
        val visibility = ProtoEnumFlags.descriptorVisibility(visibility.normalize())
        val modality = ProtoEnumFlags.modality(modality)
        val kind = if (isFakeOverride) ProtoBuf.MemberKind.FAKE_OVERRIDE else ProtoBuf.MemberKind.DECLARATION

        val flags = IrFlags.getFunctionFlags(
            hasAnnotation, visibility, modality, kind,
            isOperator, isInfix, isInline, isTailrec, isExternal, isSuspend, isExpect,
            /* hasStableParameterNames = */ true, /* hasMustUseReturnValue = */ false
            // hasStableParameterNames/hasMustUseReturnValue do not make sense for Ir, just pass the default value
        )

        return flags.toLong()
    }
}

fun FunctionFlags.Companion.encode(constructor: IrConstructor): Long {
    constructor.run {
        val hasAnnotation = annotations.isNotEmpty()
        val visibility = ProtoEnumFlags.descriptorVisibility(visibility.normalize())
        val flags = IrFlags.getConstructorFlags(hasAnnotation, visibility, isInline, isExternal, isExpect, isPrimary)

        return flags.toLong()
    }
}

fun PropertyFlags.Companion.encode(property: IrProperty): Long {
    return property.run {
        val hasAnnotation = annotations.isNotEmpty()
        val visibility = ProtoEnumFlags.descriptorVisibility(visibility.normalize())
        val modality = ProtoEnumFlags.modality(modality)
        val kind = if (isFakeOverride) ProtoBuf.MemberKind.FAKE_OVERRIDE else ProtoBuf.MemberKind.DECLARATION
        val hasGetter = getter != null
        val hasSetter = setter != null

        val flags = IrFlags.getPropertyFlags(
            hasAnnotation, visibility, modality, kind,
            isVar, hasGetter, hasSetter, false, isConst, isLateinit, isExternal, isDelegated, isExpect,
            /* hasMustUseReturnValue = */ false
        )

        flags.toLong()
    }
}

fun ValueParameterFlags.Companion.encode(param: IrValueParameter): Long {
    return param.run {
        IrFlags.getValueParameterFlags(
            annotations.isNotEmpty(),
            defaultValue != null,
            isCrossinline,
            isNoinline,
            isHidden,
            isAssignable
        ).toLong()
    }
}

fun TypeAliasFlags.Companion.encode(typeAlias: IrTypeAlias): Long {
    return typeAlias.run {
        val visibility = ProtoEnumFlags.descriptorVisibility(visibility.normalize())
        IrFlags.getTypeAliasFlags(annotations.isNotEmpty(), visibility, isActual).toLong()
    }
}

fun TypeParameterFlags.Companion.encode(typeParameter: IrTypeParameter): Long {
    return typeParameter.run {
        val variance = ProtoEnumFlags.variance(variance)
        IrFlags.getTypeParameterFlags(annotations.isNotEmpty(), variance, isReified).toLong()
    }
}

fun FieldFlags.Companion.encode(field: IrField): Long {
    return field.run {
        val visibility = ProtoEnumFlags.descriptorVisibility(visibility.normalize())
        IrFlags.getFieldFlags(annotations.isNotEmpty(), visibility, isFinal, isExternal, isStatic).toLong()
    }
}

fun LocalVariableFlags.Companion.encode(variable: IrVariable): Long {
    return variable.run {
        IrFlags.getLocalFlags(annotations.isNotEmpty(), isVar, isConst, isLateinit).toLong()
    }
}

fun LocalVariableFlags.Companion.encode(delegate: IrLocalDelegatedProperty): Long {
    return delegate.run {
        IrFlags.getLocalFlags(annotations.isNotEmpty(), isVar, false, false).toLong()
    }
}

fun RichFunctionReferenceFlags.Companion.encode(reference: IrRichFunctionReference): Long {
    return reference.run {
        HAS_UNIT_CONVERSION.toFlags(hasUnitConversion) or
                HAS_SUSPEND_CONVERSION.toFlags(hasSuspendConversion) or
                HAS_VARARG_CONVERSION.toFlags(hasVarargConversion) or
                IS_RESTRICTED_SUSPENSION.toFlags(isRestrictedSuspension)
    }.toLong()
}
