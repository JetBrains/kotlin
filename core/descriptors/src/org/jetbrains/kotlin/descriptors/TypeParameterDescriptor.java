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

package org.jetbrains.kotlin.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.mpp.TypeParameterSymbolMarker;
import org.jetbrains.kotlin.storage.StorageManager;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeConstructor;
import org.jetbrains.kotlin.types.Variance;
import org.jetbrains.kotlin.types.model.TypeParameterMarker;

import java.util.List;

public interface TypeParameterDescriptor extends ClassifierDescriptor, TypeParameterMarker, TypeParameterSymbolMarker {
    boolean isReified();

    @NotNull
    Variance getVariance();

    @NotNull
    List<KotlinType> getUpperBounds();

    @NotNull
    @Override
    TypeConstructor getTypeConstructor();

    @NotNull
    @Override
    TypeParameterDescriptor getOriginal();

    int getIndex();

    /**
     * Is current parameter just a copy of another type parameter (getOriginal) from outer declaration
     * to be used for type constructor of inner declaration (i.e. inner class).
     *
     * If this method returns true:
     * 1. Containing declaration for current parameter is the inner one
     * 2. 'getOriginal' returns original type parameter from outer declaration
     * 3. 'getTypeConstructor' is the same as for original declaration (at least in means of 'equals')
     */
    boolean isCapturedFromOuterDeclaration();

    @NotNull
    StorageManager getStorageManager();
}
