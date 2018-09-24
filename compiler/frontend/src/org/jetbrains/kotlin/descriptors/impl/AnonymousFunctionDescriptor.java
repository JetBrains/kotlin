/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.kotlin.descriptors.SourceElement;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.name.SpecialNames;

public class AnonymousFunctionDescriptor extends SimpleFunctionDescriptorImpl {
    private final boolean isSuspend;

    public AnonymousFunctionDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            @NotNull Kind kind,
            @NotNull SourceElement source,
            boolean isSuspend
    ) {
        this(containingDeclaration, null, annotations, SpecialNames.ANONYMOUS_FUNCTION, kind, source, isSuspend);
    }

    private AnonymousFunctionDescriptor(
            @NotNull DeclarationDescriptor declarationDescriptor,
            @Nullable SimpleFunctionDescriptor original,
            @NotNull Annotations annotations,
            @NotNull Name name,
            @NotNull Kind kind,
            @NotNull SourceElement source,
            boolean isSuspend
    ) {
        super(declarationDescriptor, original, annotations, name, kind, source);
        this.isSuspend = isSuspend;
    }

    @NotNull
    @Override
    protected FunctionDescriptorImpl createSubstitutedCopy(
            @NotNull DeclarationDescriptor newOwner,
            @Nullable FunctionDescriptor original,
            @NotNull Kind kind,
            @Nullable Name newName,
            @NotNull Annotations annotations,
            @NotNull SourceElement source
    ) {
        return new AnonymousFunctionDescriptor(
                newOwner,
                (SimpleFunctionDescriptor) original,
                annotations,
                newName != null ? newName : getName(),
                kind,
                source,
                isSuspend
        );
    }

    @Override
    public boolean isSuspend() {
        return isSuspend;
    }
}
