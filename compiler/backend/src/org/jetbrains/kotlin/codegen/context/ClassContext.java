/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.OwnerKind;
import org.jetbrains.kotlin.codegen.StackValue;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.types.SimpleType;

import static org.jetbrains.kotlin.codegen.AsmUtil.CAPTURED_THIS_FIELD;
import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.CLOSURE;
import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.canHaveOuter;

public class ClassContext extends FieldOwnerContext<ClassDescriptor> {
    private final KotlinTypeMapper typeMapper;

    public ClassContext(
            @NotNull KotlinTypeMapper typeMapper,
            @NotNull ClassDescriptor contextDescriptor,
            @NotNull OwnerKind contextKind,
            @Nullable CodegenContext parentContext,
            @Nullable LocalLookup localLookup
    ) {
        super(contextDescriptor, contextKind, parentContext, typeMapper.getBindingContext().get(CLOSURE, contextDescriptor),
              contextDescriptor, localLookup);

        this.typeMapper = typeMapper;
    }

    @Override
    @Nullable
    protected StackValue.Field computeOuterExpression() {
        ClassDescriptor enclosingClass = getEnclosingClass();
        if (enclosingClass == null) return null;

        if (!canHaveOuter(typeMapper.getBindingContext(), getContextDescriptor())) return null;

        SimpleType enclosingClassType = enclosingClass.getDefaultType();
        return StackValue.field(
                typeMapper.mapType(enclosingClassType),
                enclosingClassType,
                typeMapper.mapType(getContextDescriptor()),
                CAPTURED_THIS_FIELD,
                /* isStatic = */ false,
                StackValue.LOCAL_0,
                enclosingClass
        );
    }

    @Nullable
    public CodegenContext getCompanionObjectContext() {
        if (getContextDescriptor().getCompanionObjectDescriptor() != null) {
            return findChildContext(getContextDescriptor().getCompanionObjectDescriptor());
        }
        return null;
    }

    @Override
    public String toString() {
        return "Class: " + getContextDescriptor();
    }
}
