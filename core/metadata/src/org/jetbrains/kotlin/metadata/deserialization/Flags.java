/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.metadata.deserialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.metadata.ProtoBuf;
import org.jetbrains.kotlin.protobuf.Internal;

public class Flags {
    private Flags() {}

    // Types
    public static final BooleanFlagField SUSPEND_TYPE = FlagField.booleanFirst();

    // Common for declarations

    public static final BooleanFlagField HAS_ANNOTATIONS = FlagField.booleanFirst();
    public static final FlagField<ProtoBuf.Visibility> VISIBILITY = FlagField.after(HAS_ANNOTATIONS, ProtoBuf.Visibility.values());
    public static final FlagField<ProtoBuf.Modality> MODALITY = FlagField.after(VISIBILITY, ProtoBuf.Modality.values());

    // Class

    public static final FlagField<ProtoBuf.Class.Kind> CLASS_KIND = FlagField.after(MODALITY, ProtoBuf.Class.Kind.values());
    public static final BooleanFlagField IS_INNER = FlagField.booleanAfter(CLASS_KIND);
    public static final BooleanFlagField IS_DATA = FlagField.booleanAfter(IS_INNER);
    public static final BooleanFlagField IS_EXTERNAL_CLASS = FlagField.booleanAfter(IS_DATA);
    public static final BooleanFlagField IS_EXPECT_CLASS = FlagField.booleanAfter(IS_EXTERNAL_CLASS);
    public static final BooleanFlagField IS_INLINE_CLASS = FlagField.booleanAfter(IS_EXPECT_CLASS);

    // Constructors

    public static final BooleanFlagField IS_SECONDARY = FlagField.booleanAfter(VISIBILITY);

    // Callables

    public static final FlagField<ProtoBuf.MemberKind> MEMBER_KIND = FlagField.after(MODALITY, ProtoBuf.MemberKind.values());

    // Functions

    public static final BooleanFlagField IS_OPERATOR = FlagField.booleanAfter(MEMBER_KIND);
    public static final BooleanFlagField IS_INFIX = FlagField.booleanAfter(IS_OPERATOR);
    public static final BooleanFlagField IS_INLINE = FlagField.booleanAfter(IS_INFIX);
    public static final BooleanFlagField IS_TAILREC = FlagField.booleanAfter(IS_INLINE);
    public static final BooleanFlagField IS_EXTERNAL_FUNCTION = FlagField.booleanAfter(IS_TAILREC);
    public static final BooleanFlagField IS_SUSPEND = FlagField.booleanAfter(IS_EXTERNAL_FUNCTION);
    public static final BooleanFlagField IS_EXPECT_FUNCTION = FlagField.booleanAfter(IS_SUSPEND);

    // Properties

    public static final BooleanFlagField IS_VAR = FlagField.booleanAfter(MEMBER_KIND);
    public static final BooleanFlagField HAS_GETTER = FlagField.booleanAfter(IS_VAR);
    public static final BooleanFlagField HAS_SETTER = FlagField.booleanAfter(HAS_GETTER);
    public static final BooleanFlagField IS_CONST = FlagField.booleanAfter(HAS_SETTER);
    public static final BooleanFlagField IS_LATEINIT = FlagField.booleanAfter(IS_CONST);
    public static final BooleanFlagField HAS_CONSTANT = FlagField.booleanAfter(IS_LATEINIT);
    public static final BooleanFlagField IS_EXTERNAL_PROPERTY = FlagField.booleanAfter(HAS_CONSTANT);
    public static final BooleanFlagField IS_DELEGATED = FlagField.booleanAfter(IS_EXTERNAL_PROPERTY);
    public static final BooleanFlagField IS_EXPECT_PROPERTY = FlagField.booleanAfter(IS_DELEGATED);

    // Parameters

    public static final BooleanFlagField DECLARES_DEFAULT_VALUE = FlagField.booleanAfter(HAS_ANNOTATIONS);
    public static final BooleanFlagField IS_CROSSINLINE = FlagField.booleanAfter(DECLARES_DEFAULT_VALUE);
    public static final BooleanFlagField IS_NOINLINE = FlagField.booleanAfter(IS_CROSSINLINE);

    // Accessors

    public static final BooleanFlagField IS_NOT_DEFAULT = FlagField.booleanAfter(MODALITY);
    public static final BooleanFlagField IS_EXTERNAL_ACCESSOR = FlagField.booleanAfter(IS_NOT_DEFAULT);
    public static final BooleanFlagField IS_INLINE_ACCESSOR = FlagField.booleanAfter(IS_EXTERNAL_ACCESSOR);

    // Contracts expressions
    public static final BooleanFlagField IS_NEGATED = FlagField.booleanFirst();
    public static final BooleanFlagField IS_NULL_CHECK_PREDICATE = FlagField.booleanAfter(IS_NEGATED);

    // Annotations
    public static final BooleanFlagField IS_UNSIGNED = FlagField.booleanFirst();

    // ---

    public static int getTypeFlags(boolean isSuspend) {
        return SUSPEND_TYPE.toFlags(isSuspend);
    }

    public static int getClassFlags(
            boolean hasAnnotations,
            @NotNull ProtoBuf.Visibility visibility,
            @NotNull ProtoBuf.Modality modality,
            @NotNull ProtoBuf.Class.Kind kind,
            boolean inner,
            boolean isData,
            boolean isExternal,
            boolean isExpect,
            boolean isInline
    ) {
        return HAS_ANNOTATIONS.toFlags(hasAnnotations)
               | MODALITY.toFlags(modality)
               | VISIBILITY.toFlags(visibility)
               | CLASS_KIND.toFlags(kind)
               | IS_INNER.toFlags(inner)
               | IS_DATA.toFlags(isData)
               | IS_EXTERNAL_CLASS.toFlags(isExternal)
               | IS_EXPECT_CLASS.toFlags(isExpect)
               | IS_INLINE_CLASS.toFlags(isInline)
                ;
    }

    public static int getConstructorFlags(
            boolean hasAnnotations,
            @NotNull ProtoBuf.Visibility visibility,
            boolean isSecondary
    ) {
        return HAS_ANNOTATIONS.toFlags(hasAnnotations)
               | VISIBILITY.toFlags(visibility)
               | IS_SECONDARY.toFlags(isSecondary)
                ;
    }

    public static int getFunctionFlags(
            boolean hasAnnotations,
            @NotNull ProtoBuf.Visibility visibility,
            @NotNull ProtoBuf.Modality modality,
            @NotNull ProtoBuf.MemberKind memberKind,
            boolean isOperator,
            boolean isInfix,
            boolean isInline,
            boolean isTailrec,
            boolean isExternal,
            boolean isSuspend,
            boolean isExpect
    ) {
        return HAS_ANNOTATIONS.toFlags(hasAnnotations)
               | VISIBILITY.toFlags(visibility)
               | MODALITY.toFlags(modality)
               | MEMBER_KIND.toFlags(memberKind)
               | IS_OPERATOR.toFlags(isOperator)
               | IS_INFIX.toFlags(isInfix)
               | IS_INLINE.toFlags(isInline)
               | IS_TAILREC.toFlags(isTailrec)
               | IS_EXTERNAL_FUNCTION.toFlags(isExternal)
               | IS_SUSPEND.toFlags(isSuspend)
               | IS_EXPECT_FUNCTION.toFlags(isExpect)
                ;
    }

    public static int getPropertyFlags(
            boolean hasAnnotations,
            @NotNull ProtoBuf.Visibility visibility,
            @NotNull ProtoBuf.Modality modality,
            @NotNull ProtoBuf.MemberKind memberKind,
            boolean isVar,
            boolean hasGetter,
            boolean hasSetter,
            boolean hasConstant,
            boolean isConst,
            boolean lateInit,
            boolean isExternal,
            boolean isDelegated,
            boolean isExpect
    ) {
        return HAS_ANNOTATIONS.toFlags(hasAnnotations)
               | VISIBILITY.toFlags(visibility)
               | MODALITY.toFlags(modality)
               | MEMBER_KIND.toFlags(memberKind)
               | IS_VAR.toFlags(isVar)
               | HAS_GETTER.toFlags(hasGetter)
               | HAS_SETTER.toFlags(hasSetter)
               | IS_CONST.toFlags(isConst)
               | IS_LATEINIT.toFlags(lateInit)
               | HAS_CONSTANT.toFlags(hasConstant)
               | IS_EXTERNAL_PROPERTY.toFlags(isExternal)
               | IS_DELEGATED.toFlags(isDelegated)
               | IS_EXPECT_PROPERTY.toFlags(isExpect)
                ;
    }

    public static int getAccessorFlags(
            boolean hasAnnotations,
            @NotNull ProtoBuf.Visibility visibility,
            @NotNull ProtoBuf.Modality modality,
            boolean isNotDefault,
            boolean isExternal,
            boolean isInlineAccessor
    ) {
        return HAS_ANNOTATIONS.toFlags(hasAnnotations)
               | MODALITY.toFlags(modality)
               | VISIBILITY.toFlags(visibility)
               | IS_NOT_DEFAULT.toFlags(isNotDefault)
               | IS_EXTERNAL_ACCESSOR.toFlags(isExternal)
               | IS_INLINE_ACCESSOR.toFlags(isInlineAccessor)
                ;
    }

    public static int getContractExpressionFlags(boolean isNegated, boolean isNullCheckPredicate) {
        return IS_NEGATED.toFlags(isNegated)
                | IS_NULL_CHECK_PREDICATE.toFlags(isNullCheckPredicate);
    }

    public static int getValueParameterFlags(
            boolean hasAnnotations,
            boolean declaresDefaultValue,
            boolean isCrossinline,
            boolean isNoinline
    ) {
        return HAS_ANNOTATIONS.toFlags(hasAnnotations)
               | DECLARES_DEFAULT_VALUE.toFlags(declaresDefaultValue)
               | IS_CROSSINLINE.toFlags(isCrossinline)
               | IS_NOINLINE.toFlags(isNoinline)
                ;
    }

    public static int getTypeAliasFlags(boolean hasAnnotations, ProtoBuf.Visibility visibility) {
        return HAS_ANNOTATIONS.toFlags(hasAnnotations)
               | VISIBILITY.toFlags(visibility)
                ;
    }

    // Infrastructure

    public static abstract class FlagField<E> {
        public static <E extends Internal.EnumLite> FlagField<E> after(FlagField<?> previousField, E[] values) {
            int offset = previousField.offset + previousField.bitWidth;
            return new EnumLiteFlagField<E>(offset, values);
        }

        public static <E extends Internal.EnumLite> FlagField<E> first(E[] values) {
            return new EnumLiteFlagField<E>(0, values);
        }

        public static BooleanFlagField booleanFirst() {
            return new BooleanFlagField(0);
        }

        public static BooleanFlagField booleanAfter(FlagField<?> previousField) {
            int offset = previousField.offset + previousField.bitWidth;
            return new BooleanFlagField(offset);
        }

        public final int offset;
        public final int bitWidth;

        private FlagField(int offset, int bitWidth) {
            this.offset = offset;
            this.bitWidth = bitWidth;
        }

        public abstract E get(int flags);

        public abstract int toFlags(E value);
    }

    @SuppressWarnings("WeakerAccess")
    public static class BooleanFlagField extends FlagField<Boolean> {
        public BooleanFlagField(int offset) {
            super(offset, 1);
        }

        @Override
        @NotNull
        public Boolean get(int flags) {
            return (flags & (1 << offset)) != 0;
        }

        @Override
        public int toFlags(Boolean value) {
            return value ? 1 << offset : 0;
        }

        public int invert(int flags) { return (flags ^ (1 << offset)); }
    }

    private static class EnumLiteFlagField<E extends Internal.EnumLite> extends FlagField<E> {
        private final E[] values;

        public EnumLiteFlagField(int offset, E[] values) {
            super(offset, bitWidth(values));
            this.values = values;
        }

        private static <E> int bitWidth(@NotNull E[] enumEntries) {
            int length = enumEntries.length - 1;
            if (length == 0) return 1;
            for (int i = 31; i >= 0; i--) {
                if ((length & (1 << i)) != 0) return i + 1;
            }
            throw new IllegalStateException("Empty enum: " + enumEntries.getClass());
        }

        @Override
        @Nullable
        public E get(int flags) {
            int maskUnshifted = (1 << bitWidth) - 1;
            int mask = maskUnshifted << offset;
            int value = (flags & mask) >> offset;
            for (E e : values) {
                if (e.getNumber() == value) {
                    return e;
                }
            }
            return null;
        }

        @Override
        public int toFlags(E value) {
            return value.getNumber() << offset;
        }
    }
}
