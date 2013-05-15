package org.jetbrains.jet.descriptors.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;

public class Flags {
    private Flags() {}

    // Common

    public static final int VISIBILITY_BIT_COUNT = bitWidth(ProtoBuf.Visibility.values());
    public static final int VISIBILITY_OFFSET = 0;

    public static final int MODALITY_BIT_COUNT = bitWidth(ProtoBuf.Modality.values());
    public static final int MODALITY_OFFSET = VISIBILITY_OFFSET + VISIBILITY_BIT_COUNT;

    // Class

    public static final int CLASS_KIND_BIT_COUNT = bitWidth(ProtoBuf.Class.Kind.values());
    public static final int CLASS_KIND_OFFSET = MODALITY_OFFSET + MODALITY_BIT_COUNT;

    public static final int INNER_BIT_COUNT = 1;
    public static final int INNER_OFFSET = CLASS_KIND_OFFSET + CLASS_KIND_BIT_COUNT;

    // Callables

    public static final int CALLABLE_KIND_BIT_COUNT = bitWidth(ProtoBuf.Callable.CallableKind.values());
    public static final int CALLABLE_KIND_OFFSET = MODALITY_OFFSET + MODALITY_BIT_COUNT;

    public static final int MEMBER_KIND_BIT_COUNT = bitWidth(ProtoBuf.Callable.MemberKind.values());
    public static final int MEMBER_KIND_OFFSET = CALLABLE_KIND_OFFSET + CALLABLE_KIND_BIT_COUNT;

    public static final int INLINE_BIT_COUNT = 1;
    public static final int INLINE_OFFSET = MEMBER_KIND_OFFSET + MEMBER_KIND_BIT_COUNT;

    // ---

    private static final Boolean[] BOOLEANS = { false, true };

    private static <E extends Enum<E>> int bitWidth(@NotNull E[] enumEntries) {
        int length = enumEntries.length - 1;
        if (length == 0) return 1;
        for (int i = 31; i >= 0; i--) {
            if ((length & (1 << i)) != 0) return i + 1;
        }
        throw new IllegalStateException("Empty enum: " + enumEntries.getClass());
    }

    @NotNull
    private static <E> E getValue(int flags, @NotNull E[] values, int count, int offset) {
        int maskUnshifted = (1 << count) - 1;
        int mask = maskUnshifted << offset;
        int value = (flags & mask) >> offset;
        return values[value];
    }

    @NotNull
    public static ProtoBuf.Visibility getVisibility(int flags) {
        return getValue(flags, ProtoBuf.Visibility.values(), VISIBILITY_BIT_COUNT, VISIBILITY_OFFSET);
    }

    @NotNull
    public static ProtoBuf.Modality getModality(int flags) {
        return getValue(flags, ProtoBuf.Modality.values(), MODALITY_BIT_COUNT, MODALITY_OFFSET);
    }

    @NotNull
    public static ProtoBuf.Class.Kind getClassKind(int flags) {
        return getValue(flags, ProtoBuf.Class.Kind.values(), CLASS_KIND_BIT_COUNT, CLASS_KIND_OFFSET);
    }

    public static boolean isInner(int flags) {
        return getValue(flags, BOOLEANS, INNER_BIT_COUNT, INNER_OFFSET);
    }

    @NotNull
    public static ProtoBuf.Callable.CallableKind getCallableKind(int flags) {
        return getValue(flags, ProtoBuf.Callable.CallableKind.values(), CALLABLE_KIND_BIT_COUNT, CALLABLE_KIND_OFFSET);
    }

    @NotNull
    public static ProtoBuf.Callable.MemberKind getMemberKind(int flags) {
        return getValue(flags, ProtoBuf.Callable.MemberKind.values(), MEMBER_KIND_BIT_COUNT, MEMBER_KIND_OFFSET);
    }

    public static boolean isInline(int flags) {
        return getValue(flags, BOOLEANS, INLINE_BIT_COUNT, INLINE_OFFSET);
    }

    public static int getClassFlags(Visibility visibility, Modality modality, ClassKind kind, boolean inner) {
        int visibilityInt = visibility(visibility).getNumber();
        int modalityInt = modality(modality).getNumber();
        int classKindInt = classKind(kind).getNumber();
        int innerInt = inner ? 1 : 0;
        return visibilityInt << VISIBILITY_OFFSET
               | modalityInt << MODALITY_OFFSET
               | classKindInt << CLASS_KIND_OFFSET
               | innerInt << INNER_OFFSET
               ;
    }

    private static ProtoBuf.Class.Kind classKind(ClassKind kind) {
        switch (kind) {
            case CLASS:
                return ProtoBuf.Class.Kind.CLASS;
            case TRAIT:
                return ProtoBuf.Class.Kind.TRAIT;
            case ENUM_CLASS:
                return ProtoBuf.Class.Kind.ENUM_CLASS;
            case ENUM_ENTRY:
                return ProtoBuf.Class.Kind.ENUM_ENTRY;
            case ANNOTATION_CLASS:
                return ProtoBuf.Class.Kind.ANNOTATION_CLASS;
            case OBJECT:
                return ProtoBuf.Class.Kind.OBJECT;
            case CLASS_OBJECT:
                return ProtoBuf.Class.Kind.CLASS_OBJECT;
        }
        throw new IllegalArgumentException("Unknown class kind: " + kind);
    }

    public static int getCallableFlags(
            @NotNull Visibility visibility,
            @NotNull Modality modality,
            @NotNull CallableMemberDescriptor.Kind memberKind,
            @NotNull ProtoBuf.Callable.CallableKind callableKind,
            boolean inline
    ) {
        int visibilityInt = visibility(visibility).getNumber();
        int modalityInt = modality(modality).getNumber();
        int memberKindInt = memberKind(memberKind).getNumber();
        int callableKindInt = callableKind.getNumber();
        int inlineInt = inline ? 1 : 0;
        return visibilityInt << VISIBILITY_OFFSET
               | modalityInt << MODALITY_OFFSET
               | memberKindInt << MEMBER_KIND_OFFSET
               | callableKindInt << CALLABLE_KIND_OFFSET
               | inlineInt << INLINE_OFFSET
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
        else if (visibility == Visibilities.PROTECTED) {
            return ProtoBuf.Visibility.PROTECTED;
        }
        return ProtoBuf.Visibility.EXTRA;
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
        }
        throw new IllegalArgumentException("Unknown modality: " + modality);
    }

    @NotNull
    private static ProtoBuf.Callable.MemberKind memberKind(@NotNull CallableMemberDescriptor.Kind kind) {
        switch (kind) {
            case DECLARATION:
                return ProtoBuf.Callable.MemberKind.DECLARATION;
            case FAKE_OVERRIDE:
                return ProtoBuf.Callable.MemberKind.FAKE_OVERRIDE;
            case DELEGATION:
                return ProtoBuf.Callable.MemberKind.DELEGATION;
            case SYNTHESIZED:
                return ProtoBuf.Callable.MemberKind.SYNTHESIZED;
        }
        throw new IllegalArgumentException("Unknown member kind: " + kind);
    }
}
