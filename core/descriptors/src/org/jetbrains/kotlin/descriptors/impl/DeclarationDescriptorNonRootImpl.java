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

package org.jetbrains.kotlin.descriptors.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.name.Name;

import java.util.ArrayList;
import java.util.List;

public abstract class DeclarationDescriptorNonRootImpl
        extends DeclarationDescriptorImpl
        implements DeclarationDescriptorNonRoot {

    @NotNull
    private final DeclarationDescriptor containingDeclaration;

    @NotNull
    private final SourceElement source;

    private List<Runnable> initFinalizationActions;

    private boolean initFinalized;

    protected DeclarationDescriptorNonRootImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            @NotNull Name name,
            @NotNull SourceElement source
    ) {
        this(containingDeclaration, annotations, name, source, false);
    }

    protected DeclarationDescriptorNonRootImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            @NotNull Name name,
            @NotNull SourceElement source,
            boolean initFinalized
    ) {
        super(annotations, name);

        this.containingDeclaration = containingDeclaration;
        this.source = source;
        this.initFinalized = initFinalized;
    }


    @NotNull
    @Override
    public DeclarationDescriptorWithSource getOriginal() {
        return (DeclarationDescriptorWithSource) super.getOriginal();
    }

    @Override
    @NotNull
    public DeclarationDescriptor getContainingDeclaration() {
        return containingDeclaration;
    }

    @Override
    @NotNull
    public SourceElement getSource() {
        return source;
    }

    public final void finalizeInit() {
        if (initFinalized && allowReInitialization()) {
            if (initFinalizationActions != null && !initFinalizationActions.isEmpty()) {
                throw new IllegalStateException();
            }
            return;
        }

        checkInitNotFinalized();
        initFinalized = true;
        if (initFinalizationActions != null) {
            for (Runnable action : initFinalizationActions) {
                action.run();
            }
            initFinalizationActions.clear();
        }
    }

    @Override
    public void addInitFinalizationAction(Runnable action) {
        if (initFinalized) {
            action.run();
        } else {
            if (initFinalizationActions == null) {
                initFinalizationActions = new ArrayList<>();
            }
            initFinalizationActions.add(action);
        }
    }

    protected boolean allowReInitialization() {
        return false;
    }

    protected void checkInitNotFinalized() {
        if (initFinalized) {
            throw new IllegalStateException(initializedMessage());
        }
    }

    protected void checkInitFinalized() {
        if (!initFinalized) {
            throw new IllegalStateException(uninitializedMessage());
        }
    }

    protected String initializedMessage() {
        return "Initialization of " + this + " descriptor is finalized.";
    }

    protected String uninitializedMessage() {
        return "Initialization of " + this + " descriptor is not finalized.";
    }

    @Override
    public boolean isInitFinalized() {
        return initFinalized;
    }

}
