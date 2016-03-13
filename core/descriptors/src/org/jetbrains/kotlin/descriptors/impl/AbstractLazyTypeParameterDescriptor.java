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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.SourceElement;
import org.jetbrains.kotlin.descriptors.SupertypeLoopChecker;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.storage.StorageManager;
import org.jetbrains.kotlin.types.Variance;

public abstract class AbstractLazyTypeParameterDescriptor extends AbstractTypeParameterDescriptor {
    public AbstractLazyTypeParameterDescriptor(
            @NotNull StorageManager storageManager,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Name name,
            @NotNull Variance variance,
            boolean isReified,
            int index,
            @NotNull SourceElement source,
            @NotNull SupertypeLoopChecker supertypeLoopChecker
    ) {
        super(storageManager, containingDeclaration, Annotations.Companion.getEMPTY() /* TODO */, name, variance, isReified, index, source,
              supertypeLoopChecker);
    }

    @Override
    public String toString() {
        // Not using descriptor renderer to preserve laziness
        return String.format(
                "%s%s%s",
                isReified() ? "reified " : "",
                getVariance() == Variance.INVARIANT ? "" : getVariance() + " ",
                getName()
        );
    }
}
