/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
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

    // Properties

    public static final BooleanFlagField IS_VAR = FlagField.booleanAfter(MEMBER_KIND);
    public static final BooleanFlagField HAS_GETTER = FlagField.booleanAfter(IS_VAR);
    public static final BooleanFlagField HAS_SETTER = FlagField.booleanAfter(HAS_GETTER);
    public static final BooleanFlagField IS_CONST = FlagField.booleanAfter(HAS_SETTER);
    public static final BooleanFlagField IS_LATEINIT = FlagField.booleanAfter(IS_CONST);
    public static final BooleanFlagField HAS_CONSTANT = FlagField.booleanAfter(IS_LATEINIT);
    public static final BooleanFlagField IS_EXTERNAL_PROPERTY = FlagField.booleanAfter(HAS_CONSTANT);
    public static final BooleanFlagField IS_DELEGATED = FlagField.booleanAfter(IS_EXTERNAL_PROPERTY);

    // Parameters

    public static final BooleanFlagField DECLARES_DEFAULT_VALUE = FlagField.booleanAfter(HAS_ANNOTATIONS);
    public static final BooleanFlagField IS_CROSSINLINE = FlagField.booleanAfter(DECLARES_DEFAULT_VALUE);
    public static final BooleanFlagField IS_NOINLINE = FlagField.booleanAfter(IS_CROSSINLINE);

    // Accessors

    public static final BooleanFlagField IS_NOT_DEFAULT = FlagField.booleanAfter(MODALITY);
    public static final BooleanFlagField IS_EXTERNAL_ACCESSOR = FlagField.booleanAfter(IS_NOT_DEFAULT);
    public static final BooleanFlagField IS_INLINE_ACCESSOR = FlagField.booleanAfter(IS_EXTERNAL_ACCESSOR);

    // ---

    public static int getTypeFlags(boolean isSuspend) {
        return SUSPEND_TYPE.toFlags(isSuspend);
    }

    public static int getClassFlags(
            boolean hasAnnotations,
            Visibility visibility,
            Modality modality,
            ClassKind kind,
            boolean inner,
            boolean isCompanionObject,
            boolean isData,
            boolean isExternal
    ) {
        return HAS_ANNOTATIONS.toFlags(hasAnnotations)
               | MODALITY.toFlags(modality(modality))
               | VISIBILITY.toFlags(visibility(visibility))
               | CLASS_KIND.toFlags(classKind(kind, isCompanionObject))
               | IS_INNER.toFlags(inner)
               | IS_DATA.toFlags(isData)
               | IS_EXTERNAL_CLASS.toFlags(isExternal)
               ;
    }

    private static ProtoBuf.Class.Kind classKind(ClassKind kind, boolean isCompanionObject) {
        if (isCompanionObject) return ProtoBuf.Class.Kind.COMPANION_OBJECT;

        switch (kind) {
            case CLASS:
                return ProtoBuf.Class.Kind.CLASS;
            case INTERFACE:
                return ProtoBuf.Class.Kind.INTERFACE;
            case ENUM_CLASS:
                return ProtoBuf.Class.Kind.ENUM_CLASS;
            case ENUM_ENTRY:
                return ProtoBuf.Class.Kind.ENUM_ENTRY;
            case ANNOTATION_CLASS:
                return ProtoBuf.Class.Kind.ANNOTATION_CLASS;
            case OBJECT:
                return ProtoBuf.Class.Kind.OBJECT;
        }
        throw new IllegalArgumentException("Unknown class kind: " + kind);
    }

    public static int getConstructorFlags(
            boolean hasAnnotations,
            @NotNull Visibility visibility,
            boolean isSecondary
    ) {
        return HAS_ANNOTATIONS.toFlags(hasAnnotations)
               | VISIBILITY.toFlags(visibility(visibility))
               | IS_SECONDARY.toFlags(isSecondary)
                ;
    }

    public static int getFunctionFlags(
            boolean hasAnnotations,
            @NotNull Visibility visibility,
            @NotNull Modality modality,
            @NotNull CallableMemberDescriptor.Kind memberKind,
            boolean isOperator,
            boolean isInfix,
            boolean isInline,
            boolean isTailrec,
            boolean isExternal,
            boolean isSuspend
    ) {
        return HAS_ANNOTATIONS.toFlags(hasAnnotations)
               | VISIBILITY.toFlags(visibility(visibility))
               | MODALITY.toFlags(modality(modality))
               | MEMBER_KIND.toFlags(memberKind(memberKind))
               | IS_OPERATOR.toFlags(isOperator)
               | IS_INFIX.toFlags(isInfix)
               | IS_INLINE.toFlags(isInline)
               | IS_TAILREC.toFlags(isTailrec)
               | IS_EXTERNAL_FUNCTION.toFlags(isExternal)
               | IS_SUSPEND.toFlags(isSuspend)
                ;
    }

    public static int getPropertyFlags(
            boolean hasAnnotations,
            @NotNull Visibility visibility,
            @NotNull Modality modality,
            @NotNull CallableMemberDescriptor.Kind memberKind,
            boolean isVar,
            boolean hasGetter,
            boolean hasSetter,
            boolean hasConstant,
            boolean isConst,
            boolean lateInit,
            boolean isExternal,
            boolean isDelegated
    ) {
        return HAS_ANNOTATIONS.toFlags(hasAnnotations)
               | VISIBILITY.toFlags(visibility(visibility))
               | MODALITY.toFlags(modality(modality))
               | MEMBER_KIND.toFlags(memberKind(memberKind))
               | IS_VAR.toFlags(isVar)
               | HAS_GETTER.toFlags(hasGetter)
               | HAS_SETTER.toFlags(hasSetter)
               | IS_CONST.toFlags(isConst)
               | IS_LATEINIT.toFlags(lateInit)
               | HAS_CONSTANT.toFlags(hasConstant)
               | IS_EXTERNAL_PROPERTY.toFlags(isExternal)
               | IS_DELEGATED.toFlags(isDelegated)
                ;
    }

    public static int getAccessorFlags(
            boolean hasAnnotations,
            @NotNull Visibility visibility,
            @NotNull Modality modality,
            boolean isNotDefault,
            boolean isExternal,
            boolean isInlineAccessor
    ) {
        return HAS_ANNOTATIONS.toFlags(hasAnnotations)
               | MODALITY.toFlags(modality(modality))
               | VISIBILITY.toFlags(visibility(visibility))
               | IS_NOT_DEFAULT.toFlags(isNotDefault)
               | IS_EXTERNAL_ACCESSOR.toFlags(isExternal)
               | IS_INLINE_ACCESSOR.toFlags(isInlineAccessor)
               ;
    }

    @NotNull
    private static ProtoBuf.Visibility visibility(@NotNull Visibility visibility) {
        if (visibility == Visibilities.INTERNAL) {
            return ProtoBuf.Visibility.INTERNAL;
        }
        else if (visibility == Visibilities.PUBLIC) {
            return ProtoBuf.Visibility.PUBLIC;
        }
        else if (visibility == Visibilities.PRIVATE) {
            return ProtoBuf.Visibility.PRIVATE;
        }
        else if (visibility == Visibilities.PRIVATE_TO_THIS) {
            return ProtoBuf.Visibility.PRIVATE_TO_THIS;
        }
        else if (visibility == Visibilities.PROTECTED) {
            return ProtoBuf.Visibility.PROTECTED;
        }
        else if (visibility == Visibilities.LOCAL) {
            return ProtoBuf.Visibility.LOCAL;
        }
        throw new IllegalArgumentException("Unknown visibility: " + visibility);
    }

    @NotNull
    private static ProtoBuf.Modality modality(@NotNull Modality modality) {
        switch (modality) {
            case FINAL:
                return ProtoBuf.Modality.FINAL;
            case OPEN:
                return ProtoBuf.Modality.OPEN;
            case ABSTRACT:
                return ProtoBuf.Modality.ABSTRACT;
            case SEALED:
                return ProtoBuf.Modality.SEALED;
        }
        throw new IllegalArgumentException("Unknown modality: " + modality);
    }

    @NotNull
    private static ProtoBuf.MemberKind memberKind(@NotNull CallableMemberDescriptor.Kind kind) {
        switch (kind) {
            case DECLARATION:
                return ProtoBuf.MemberKind.DECLARATION;
            case FAKE_OVERRIDE:
                return ProtoBuf.MemberKind.FAKE_OVERRIDE;
            case DELEGATION:
                return ProtoBuf.MemberKind.DELEGATION;
            case SYNTHESIZED:
                return ProtoBuf.MemberKind.SYNTHESIZED;
        }
        throw new IllegalArgumentException("Unknown member kind: " + kind);
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

    public static int getTypeAliasFlags(boolean hasAnnotations, Visibility visibility) {
        return HAS_ANNOTATIONS.toFlags(hasAnnotations)
                | VISIBILITY.toFlags(visibility(visibility))
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

        protected final int offset;
        protected final int bitWidth;

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
