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
        implements DeclarationDescriptorNonRoot, InitializableDescriptor {

    @NotNull
    private final DeclarationDescriptor containingDeclaration;

    @NotNull
    private final SourceElement source;

    public final Exception created = new Exception();
    private final Object initLock = new Object();
    private volatile boolean initFinalized;
    private List<Runnable> initFinalizationActions;
    private List<InitializableDescriptor> dependencies;

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

    @Override
    public final void finalizeInit() {
        if (containingDeclaration instanceof InitializableDescriptor && !((InitializableDescriptor)containingDeclaration).isInitFinalized()) {
            throw new IllegalStateException(uninitializedMessage((InitializableDescriptor) containingDeclaration));
        }
        List<Runnable> finalizationActions;
        List<InitializableDescriptor> dependencyList;
        synchronized (initLock) {
            finalizationActions = initFinalizationActions != null ? new ArrayList<>(initFinalizationActions) : null;
            if (initFinalized && allowReInitialization()) {
                if (finalizationActions != null && !finalizationActions.isEmpty()) {
                    throw new IllegalStateException();
                }
                return;
            }

            checkInitNotFinalized();
            dependencyList = dependencies != null ? new ArrayList<>(dependencies) : null;

            initFinalized = true;
        }
        if (dependencyList != null) {
            for (InitializableDescriptor descriptor : dependencyList) {
                descriptor.finalizeInit();
            }
        }
        if (finalizationActions != null) {
            for (Runnable action : finalizationActions) {
                action.run();
            }
            finalizationActions.clear();
        }
    }

    @Override
    public void addDependency(@NotNull InitializableDescriptor dependency) {
        if (initFinalized) {
            dependency.finalizeInit();
        } else {
            synchronized (initLock) {
                if (dependencies == null) {
                    dependencies = new ArrayList<>();
                }
                dependencies.add(dependency);
            }
        }
    }

    @Override
    public void addInitFinalizationAction(Runnable action) {
        if (initFinalized) {
            action.run();
        } else {
            synchronized (initLock) {
                if (initFinalizationActions == null) {
                    initFinalizationActions = new ArrayList<>();
                }
                initFinalizationActions.add(action);
            }
        }
    }

    protected boolean allowReInitialization() {
        return false;
    }

    protected void checkInitNotFinalized() {
        if (initFinalized) {
            throw new IllegalStateException(initializedMessage(this), created);
        }
    }

    protected void checkInitFinalized() {
        if (!initFinalized) {
            throw new IllegalStateException(uninitializedMessage(this));
        }
    }

    private static String initializedMessage(InitializableDescriptor descriptor) {
        return "Initialization of " + descriptor + " descriptor is finalized.";
    }

    private static String uninitializedMessage(InitializableDescriptor descriptor) {
        return "Initialization of " + descriptor + " descriptor is not finalized.";
    }

    @Override
    public boolean isInitFinalized() {
        return initFinalized;
    }

}
