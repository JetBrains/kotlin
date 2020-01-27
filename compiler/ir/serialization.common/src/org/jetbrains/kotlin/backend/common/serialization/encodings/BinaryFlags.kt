/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.encodings

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.serialization.deserialization.ProtoEnumFlags
import org.jetbrains.kotlin.types.Variance

inline class ClassFlags(val flags: Long) {

    val modality: Modality get() = ProtoEnumFlags.modality(Flags.MODALITY.get(flags.toInt()))
    val visibility: Visibility get() = ProtoEnumFlags.visibility(Flags.VISIBILITY.get(flags.toInt()))
    val kind: ClassKind get() = ProtoEnumFlags.classKind(Flags.CLASS_KIND.get(flags.toInt()))

    val isCompanion: Boolean get() = Flags.CLASS_KIND.get(flags.toInt()) == ProtoBuf.Class.Kind.COMPANION_OBJECT
    val isInner: Boolean get() = Flags.IS_INNER.get(flags.toInt())
    val isData: Boolean get() = Flags.IS_DATA.get(flags.toInt())
    val isInline: Boolean get() = Flags.IS_INLINE_CLASS.get(flags.toInt())
    val isExpect: Boolean get() = Flags.IS_EXPECT_CLASS.get(flags.toInt())
    val isExternal: Boolean get() = Flags.IS_EXTERNAL_CLASS.get(flags.toInt())
    val isFun: Boolean get() = Flags.IS_FUN_INTERFACE.get(flags.toInt())

    companion object {
        fun encode(clazz: IrClass): Long {
            return clazz.run {
                val hasAnnotation = annotations.isNotEmpty()
                val visibility = ProtoEnumFlags.visibility(visibility)
                val modality = ProtoEnumFlags.modality(modality)
                val kind = ProtoEnumFlags.classKind(kind, isCompanion)

                val flags =
                    Flags.getClassFlags(hasAnnotation, visibility, modality, kind, isInner, isData, isExternal, isExpect, isInline, isFun)

                flags.toLong()
            }
        }

        fun decode(code: Long) = ClassFlags(code)
    }
}

inline class FunctionFlags(val flags: Long) {

    val modality: Modality get() = ProtoEnumFlags.modality(Flags.MODALITY.get(flags.toInt()))
    val visibility: Visibility get() = ProtoEnumFlags.visibility(Flags.VISIBILITY.get(flags.toInt()))

    val isOperator: Boolean get() = Flags.IS_OPERATOR.get(flags.toInt())
    val isInline: Boolean get() = Flags.IS_INLINE.get(flags.toInt())
    val isTailrec: Boolean get() = Flags.IS_TAILREC.get(flags.toInt())
    val isExternal: Boolean get() = Flags.IS_EXTERNAL_FUNCTION.get(flags.toInt())
    val isSuspend: Boolean get() = Flags.IS_SUSPEND.get(flags.toInt())
    val isExpect: Boolean get() = Flags.IS_EXPECT_FUNCTION.get(flags.toInt())
    val isFakeOverride: Boolean get() = kind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE

    val isPrimary: Boolean get() = Flags.IS_PRIMARY.get(flags.toInt())

    private fun kind(): CallableMemberDescriptor.Kind = ProtoEnumFlags.memberKind(Flags.MEMBER_KIND.get(flags.toInt()))

    companion object {
        fun encode(function: IrSimpleFunction): Long {
            function.run {
                val hasAnnotation = annotations.isNotEmpty()
                val visibility = ProtoEnumFlags.visibility(visibility)
                val modality = ProtoEnumFlags.modality(modality)
                val kind = if (isFakeOverride) ProtoBuf.MemberKind.FAKE_OVERRIDE else ProtoBuf.MemberKind.DECLARATION


                val flags = Flags.getFunctionFlags(
                    hasAnnotation, visibility, modality, kind,
                    isOperator, false, isInline, isTailrec, isExternal, isSuspend, isExpect
                )

                return flags.toLong()
            }
        }

        fun encode(constructor: IrConstructor): Long {
            constructor.run {
                val hasAnnotation = annotations.isNotEmpty()
                val visibility = ProtoEnumFlags.visibility(visibility)
                val flags = Flags.getConstructorFlags(hasAnnotation, visibility, isInline, isExternal, isExpect, isPrimary)

                return flags.toLong()
            }
        }

        fun decode(code: Long) = FunctionFlags(code)
    }
}

inline class PropertyFlags(val flags: Long) {

    val modality: Modality get() = ProtoEnumFlags.modality(Flags.MODALITY.get(flags.toInt()))
    val visibility: Visibility get() = ProtoEnumFlags.visibility(Flags.VISIBILITY.get(flags.toInt()))

    val isVar: Boolean get() = Flags.IS_VAR.get(flags.toInt())
    val isConst: Boolean get() = Flags.IS_CONST.get(flags.toInt())
    val isLateinit: Boolean get() = Flags.IS_LATEINIT.get(flags.toInt())
    val isExternal: Boolean get() = Flags.IS_EXTERNAL_PROPERTY.get(flags.toInt())
    val isDelegated: Boolean get() = Flags.IS_DELEGATED.get(flags.toInt())
    val isExpect: Boolean get() = Flags.IS_EXPECT_PROPERTY.get(flags.toInt())
    val isFakeOverride: Boolean get() = kind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE

    private fun kind(): CallableMemberDescriptor.Kind = ProtoEnumFlags.memberKind(Flags.MEMBER_KIND.get(flags.toInt()))

    companion object {
        fun encode(property: IrProperty): Long {
            return property.run {
                val hasAnnotation = annotations.isNotEmpty()
                val visibility = ProtoEnumFlags.visibility(visibility)
                val modality = ProtoEnumFlags.modality(modality)
                val kind = if (isFakeOverride) ProtoBuf.MemberKind.FAKE_OVERRIDE else ProtoBuf.MemberKind.DECLARATION
                val hasGetter = getter != null
                val hasSetter = setter != null

                val flags = Flags.getPropertyFlags(
                    hasAnnotation, visibility, modality, kind,
                    isVar, hasGetter, hasSetter, false, isConst, isLateinit, isExternal, isDelegated, isExpect
                )

                flags.toLong()
            }
        }

        fun decode(code: Long) = PropertyFlags(code)
    }
}

inline class ValueParameterFlags(val flags: Long) {

    val isCrossInline: Boolean get() = Flags.IS_CROSSINLINE.get(flags.toInt())
    val isNoInline: Boolean get() = Flags.IS_NOINLINE.get(flags.toInt())

    companion object {
        fun encode(param: IrValueParameter): Long {
            return param.run {
                Flags.getValueParameterFlags(annotations.isNotEmpty(), defaultValue != null, isCrossinline, isNoinline).toLong()
            }
        }

        fun decode(code: Long) = ValueParameterFlags(code)
    }
}

inline class TypeAliasFlags(val flags: Long) {

    val visibility: Visibility get() = ProtoEnumFlags.visibility(Flags.VISIBILITY.get(flags.toInt()))
    val isActual: Boolean get() = Flags.IS_ACTUAL.get(flags.toInt())

    companion object {
        fun encode(typeAlias: IrTypeAlias): Long {
            return typeAlias.run {
                val visibility = ProtoEnumFlags.visibility(visibility)
                Flags.getTypeAliasFlags(annotations.isNotEmpty(), visibility, isActual).toLong()
            }
        }

        fun decode(code: Long) = TypeAliasFlags(code)
    }
}

inline class TypeParameterFlags(val flags: Long) {

    val variance: Variance get() = ProtoEnumFlags.variance(Flags.VARIANCE.get(flags.toInt()))
    val isReified: Boolean get() = Flags.IS_REIFIED.get(flags.toInt())

    companion object {
        fun encode(typeParameter: IrTypeParameter): Long {
            return typeParameter.run {
                val variance = ProtoEnumFlags.variance(variance)
                Flags.getTypeParameterFlags(annotations.isNotEmpty(), variance, isReified).toLong()
            }
        }

        fun decode(code: Long) = TypeParameterFlags(code)
    }
}

inline class FieldFlags(val flags: Long) {

    val visibility: Visibility get() = ProtoEnumFlags.visibility(Flags.VISIBILITY.get(flags.toInt()))
    val isFinal: Boolean get() = Flags.IS_FINAL.get(flags.toInt())
    val isExternal: Boolean get() = Flags.IS_EXTERNAL_FIELD.get(flags.toInt())
    val isStatic: Boolean get() = Flags.IS_STATIC.get(flags.toInt())
    val isFakeOverride: Boolean get() = Flags.IS_FAKE_OVERRIDE.get(flags.toInt())

    companion object {
        fun encode(field: IrField): Long {
            return field.run {
                val visibility = ProtoEnumFlags.visibility(visibility)
                Flags.getFieldFlags(annotations.isNotEmpty(), visibility, isFinal, isExternal, isStatic, isFakeOverride).toLong()
            }
        }

        fun decode(code: Long) = FieldFlags(code)
    }
}

inline class LocalVariableFlags(val flags: Long) {

    val isVar: Boolean get() = Flags.IS_LOCAL_VAR.get(flags.toInt())
    val isConst: Boolean get() = Flags.IS_LOCAL_CONST.get(flags.toInt())
    val isLateinit: Boolean get() = Flags.IS_LOCAL_LATEINIT.get(flags.toInt())

    companion object {
        fun encode(variable: IrVariable): Long {
            return variable.run {
                Flags.getLocalFlags(annotations.isNotEmpty(), isVar, isConst, isLateinit).toLong()
            }
        }

        fun encode(delegate: IrLocalDelegatedProperty): Long {
            return delegate.run {
                Flags.getLocalFlags(annotations.isNotEmpty(), isVar, false, false).toLong()
            }
        }

        fun decode(code: Long) = LocalVariableFlags(code)
    }
}