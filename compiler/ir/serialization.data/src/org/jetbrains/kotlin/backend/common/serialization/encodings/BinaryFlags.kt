/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.encodings

import org.jetbrains.kotlin.backend.common.serialization.IrFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.serialization.deserialization.ProtoEnumFlags
import org.jetbrains.kotlin.serialization.deserialization.descriptorVisibility
import org.jetbrains.kotlin.serialization.deserialization.memberKind
import org.jetbrains.kotlin.types.Variance

@JvmInline
value class ClassFlags(val flags: Long) {

    val modality: Modality get() = ProtoEnumFlags.modality(IrFlags.MODALITY.get(flags.toInt()))
    val visibility: DescriptorVisibility get() = ProtoEnumFlags.descriptorVisibility(IrFlags.VISIBILITY.get(flags.toInt()))
    val kind: ClassKind get() = ProtoEnumFlags.classKind(IrFlags.CLASS_KIND.get(flags.toInt()))

    val isCompanion: Boolean get() = IrFlags.CLASS_KIND.get(flags.toInt()) == ProtoBuf.Class.Kind.COMPANION_OBJECT
    val isInner: Boolean get() = IrFlags.IS_INNER.get(flags.toInt())
    val isData: Boolean get() = IrFlags.IS_DATA.get(flags.toInt())
    val isValue: Boolean get() = IrFlags.IS_VALUE_CLASS.get(flags.toInt())
    val isExpect: Boolean get() = IrFlags.IS_EXPECT_CLASS.get(flags.toInt())
    val isExternal: Boolean get() = IrFlags.IS_EXTERNAL_CLASS.get(flags.toInt())
    val isFun: Boolean get() = IrFlags.IS_FUN_INTERFACE.get(flags.toInt())
    val hasEnumEntries: Boolean get() = IrFlags.HAS_ENUM_ENTRIES.get(flags.toInt())

    companion object {
        fun decode(code: Long) = ClassFlags(code)
    }
}

@JvmInline
value class FunctionFlags(val flags: Long) {

    val modality: Modality get() = ProtoEnumFlags.modality(IrFlags.MODALITY.get(flags.toInt()))
    val visibility: DescriptorVisibility get() = ProtoEnumFlags.descriptorVisibility(IrFlags.VISIBILITY.get(flags.toInt()))

    val isOperator: Boolean get() = IrFlags.IS_OPERATOR.get(flags.toInt())
    val isInfix: Boolean get() = IrFlags.IS_INFIX.get(flags.toInt())
    val isInline: Boolean get() = IrFlags.IS_INLINE.get(flags.toInt())
    val isTailrec: Boolean get() = IrFlags.IS_TAILREC.get(flags.toInt())
    val isExternal: Boolean get() = IrFlags.IS_EXTERNAL_FUNCTION.get(flags.toInt())
    val isSuspend: Boolean get() = IrFlags.IS_SUSPEND.get(flags.toInt())
    val isExpect: Boolean get() = IrFlags.IS_EXPECT_FUNCTION.get(flags.toInt())
    val isFakeOverride: Boolean get() = kind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE

    val isPrimary: Boolean get() = IrFlags.IS_PRIMARY.get(flags.toInt())

    private fun kind(): CallableMemberDescriptor.Kind = ProtoEnumFlags.memberKind(IrFlags.MEMBER_KIND.get(flags.toInt()))

    companion object {
        fun decode(code: Long) = FunctionFlags(code)
    }
}

@JvmInline
value class PropertyFlags(val flags: Long) {

    val modality: Modality get() = ProtoEnumFlags.modality(IrFlags.MODALITY.get(flags.toInt()))
    val visibility: DescriptorVisibility get() = ProtoEnumFlags.descriptorVisibility(IrFlags.VISIBILITY.get(flags.toInt()))

    val isVar: Boolean get() = IrFlags.IS_VAR.get(flags.toInt())
    val isConst: Boolean get() = IrFlags.IS_CONST.get(flags.toInt())
    val isLateinit: Boolean get() = IrFlags.IS_LATEINIT.get(flags.toInt())
    val isExternal: Boolean get() = IrFlags.IS_EXTERNAL_PROPERTY.get(flags.toInt())
    val isDelegated: Boolean get() = IrFlags.IS_DELEGATED.get(flags.toInt())
    val isExpect: Boolean get() = IrFlags.IS_EXPECT_PROPERTY.get(flags.toInt())
    val isFakeOverride: Boolean get() = kind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE

    private fun kind(): CallableMemberDescriptor.Kind = ProtoEnumFlags.memberKind(IrFlags.MEMBER_KIND.get(flags.toInt()))

    companion object {
        fun decode(code: Long) = PropertyFlags(code)
    }
}

@JvmInline
value class ValueParameterFlags(val flags: Long) {

    val isCrossInline: Boolean get() = IrFlags.IS_CROSSINLINE.get(flags.toInt())
    val isNoInline: Boolean get() = IrFlags.IS_NOINLINE.get(flags.toInt())
    val isHidden: Boolean get() = IrFlags.IS_HIDDEN.get(flags.toInt())
    val isAssignable: Boolean get() = IrFlags.IS_ASSIGNABLE.get(flags.toInt())

    companion object {
        fun decode(code: Long) = ValueParameterFlags(code)
    }
}

@JvmInline
value class TypeAliasFlags(val flags: Long) {

    val visibility: DescriptorVisibility get() = ProtoEnumFlags.descriptorVisibility(IrFlags.VISIBILITY.get(flags.toInt()))
    val isActual: Boolean get() = IrFlags.IS_ACTUAL.get(flags.toInt())

    companion object {
        fun decode(code: Long) = TypeAliasFlags(code)
    }
}

@JvmInline
value class TypeParameterFlags(val flags: Long) {

    val variance: Variance get() = ProtoEnumFlags.variance(IrFlags.VARIANCE.get(flags.toInt()))
    val isReified: Boolean get() = IrFlags.IS_REIFIED.get(flags.toInt())

    companion object {
        fun decode(code: Long) = TypeParameterFlags(code)
    }
}

@JvmInline
value class FieldFlags(val flags: Long) {

    val visibility: DescriptorVisibility get() = ProtoEnumFlags.descriptorVisibility(IrFlags.VISIBILITY.get(flags.toInt()))
    val isFinal: Boolean get() = IrFlags.IS_FINAL.get(flags.toInt())
    val isExternal: Boolean get() = IrFlags.IS_EXTERNAL_FIELD.get(flags.toInt())
    val isStatic: Boolean get() = IrFlags.IS_STATIC.get(flags.toInt())

    companion object {
        fun decode(code: Long) = FieldFlags(code)
    }
}

@JvmInline
value class LocalVariableFlags(val flags: Long) {

    val isVar: Boolean get() = IrFlags.IS_LOCAL_VAR.get(flags.toInt())
    val isConst: Boolean get() = IrFlags.IS_LOCAL_CONST.get(flags.toInt())
    val isLateinit: Boolean get() = IrFlags.IS_LOCAL_LATEINIT.get(flags.toInt())

    companion object {
        fun decode(code: Long) = LocalVariableFlags(code)
    }
}

@JvmInline
value class RichFunctionReferenceFlags(val flags: Long) {
    val hasUnitConversion: Boolean get() = HAS_UNIT_CONVERSION.get(flags.toInt())
    val hasSuspendConversion: Boolean get() = HAS_SUSPEND_CONVERSION.get(flags.toInt())
    val hasVarargConversion: Boolean get() = HAS_VARARG_CONVERSION.get(flags.toInt())
    val isRestrictedSuspension: Boolean get() = IS_RESTRICTED_SUSPENSION.get(flags.toInt())

    companion object {
        val HAS_UNIT_CONVERSION: Flags.BooleanFlagField = Flags.FlagField.booleanFirst()
        val HAS_SUSPEND_CONVERSION: Flags.BooleanFlagField = Flags.FlagField.booleanAfter(HAS_UNIT_CONVERSION)
        val HAS_VARARG_CONVERSION: Flags.BooleanFlagField = Flags.FlagField.booleanAfter(HAS_SUSPEND_CONVERSION)
        val IS_RESTRICTED_SUSPENSION: Flags.BooleanFlagField = Flags.FlagField.booleanAfter(HAS_VARARG_CONVERSION)

        fun decode(code: Long) = RichFunctionReferenceFlags(code)
    }
}
