/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.metadata.ProtoBuf;
import org.jetbrains.kotlin.metadata.deserialization.Flags;

public class IrFlags extends Flags {
    private IrFlags() {}

    public static final BooleanFlagField IS_PRIMARY = FlagField.booleanAfter(IS_EXPECT_FUNCTION);

    // Type Aliases
    public static final BooleanFlagField IS_ACTUAL = FlagField.booleanAfter(VISIBILITY);

    // Type Parameters
    public static final FlagField<ProtoBuf.TypeParameter.Variance> VARIANCE = FlagField.after(HAS_ANNOTATIONS, ProtoBuf.TypeParameter.Variance.values());
    public static final BooleanFlagField IS_REIFIED = FlagField.booleanAfter(VARIANCE);

    // Fields
    public static final BooleanFlagField IS_FINAL = FlagField.booleanAfter(VISIBILITY);
    public static final BooleanFlagField IS_EXTERNAL_FIELD = FlagField.booleanAfter(IS_FINAL);
    public static final BooleanFlagField IS_STATIC = FlagField.booleanAfter(IS_EXTERNAL_FIELD);
    public static final BooleanFlagField IS_FAKE_OVERRIDE = FlagField.booleanAfter(IS_STATIC);

    // Parameters

    public static final BooleanFlagField IS_HIDDEN = FlagField.booleanAfter(IS_NOINLINE);
    public static final BooleanFlagField IS_ASSIGNABLE = FlagField.booleanAfter(IS_HIDDEN);

    // Local variables
    public static final BooleanFlagField IS_LOCAL_VAR = FlagField.booleanAfter(HAS_ANNOTATIONS);
    public static final BooleanFlagField IS_LOCAL_CONST = FlagField.booleanAfter(IS_LOCAL_VAR);
    public static final BooleanFlagField IS_LOCAL_LATEINIT = FlagField.booleanAfter(IS_LOCAL_CONST);


    public static int getConstructorFlags(
            boolean hasAnnotations,
            @NotNull ProtoBuf.Visibility visibility,
            boolean isInline,
            boolean isExternal,
            boolean isExpect,
            boolean isPrimary
    ) {
        return HAS_ANNOTATIONS.toFlags(hasAnnotations)
               | VISIBILITY.toFlags(visibility)
               | IS_INLINE.toFlags(isInline)
               | IS_EXTERNAL_FUNCTION.toFlags(isExternal)
               | IS_EXPECT_FUNCTION.toFlags(isExpect)
               | IS_PRIMARY.toFlags(isPrimary)
                ;
    }

    public static int getTypeAliasFlags(boolean hasAnnotations, ProtoBuf.Visibility visibility, boolean isActual) {
        return HAS_ANNOTATIONS.toFlags(hasAnnotations)
               | VISIBILITY.toFlags(visibility)
               | IS_ACTUAL.toFlags(isActual)
                ;
    }

    public static int getTypeParameterFlags(boolean hasAnnotations, ProtoBuf.TypeParameter.Variance variance, boolean isReified) {
        return HAS_ANNOTATIONS.toFlags(hasAnnotations)
               | VARIANCE.toFlags(variance)
               | IS_REIFIED.toFlags(isReified)
                ;
    }

    public static int getFieldFlags(
            boolean hasAnnotations,
            ProtoBuf.Visibility visibility,
            boolean isFinal,
            boolean isExternal,
            boolean isStatic
    ) {
        return HAS_ANNOTATIONS.toFlags(hasAnnotations)
               | VISIBILITY.toFlags(visibility)
               | IS_FINAL.toFlags(isFinal)
               | IS_EXTERNAL_FIELD.toFlags(isExternal)
               | IS_STATIC.toFlags(isStatic)
                ;
    }

    public static int getValueParameterFlags(
            boolean hasAnnotations,
            boolean declaresDefaultValue,
            boolean isCrossinline,
            boolean isNoinline,
            boolean isHidden,
            boolean isAssignable
    ) {
        return HAS_ANNOTATIONS.toFlags(hasAnnotations)
               | DECLARES_DEFAULT_VALUE.toFlags(declaresDefaultValue)
               | IS_CROSSINLINE.toFlags(isCrossinline)
               | IS_NOINLINE.toFlags(isNoinline)
               | IS_HIDDEN.toFlags(isHidden)
               | IS_ASSIGNABLE.toFlags(isAssignable)
                ;
    }

    public static int getLocalFlags(boolean hasAnnotations, boolean isVar, boolean isConst, boolean isLateinit) {
        return HAS_ANNOTATIONS.toFlags(hasAnnotations)
               | IS_LOCAL_VAR.toFlags(isVar)
               | IS_LOCAL_CONST.toFlags(isConst)
               | IS_LOCAL_LATEINIT.toFlags(isLateinit)
                ;
    }
}
