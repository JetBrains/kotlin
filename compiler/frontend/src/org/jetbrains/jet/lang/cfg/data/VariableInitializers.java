/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.cfg.data;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetProperty;

import java.util.Set;

/**
* @author svtk
*/
public class VariableInitializers {
    private final Set<JetElement> possibleLocalInitializers = Sets.newHashSet();
    private boolean isInitialized;
    private boolean isDeclared;

    public VariableInitializers(boolean isInitialized) {
        this(isInitialized, false);
    }

    public VariableInitializers(boolean isInitialized, boolean isDeclared) {
        this.isInitialized = isInitialized;
        this.isDeclared = isDeclared;
    }

    public VariableInitializers(JetElement element, @Nullable VariableInitializers previous) {
        isInitialized = true;
        isDeclared = element instanceof JetProperty || (previous != null && previous.isDeclared());
        possibleLocalInitializers.add(element);
    }

    public VariableInitializers(Set<VariableInitializers> edgesData) {
        isInitialized = true;
        isDeclared = true;
        for (VariableInitializers edgeData : edgesData) {
            if (!edgeData.isInitialized) {
                isInitialized = false;
            }
            if (!edgeData.isDeclared) {
                isDeclared = false;
            }
            possibleLocalInitializers.addAll(edgeData.possibleLocalInitializers);
        }
    }

    public Set<JetElement> getPossibleLocalInitializers() {
        return possibleLocalInitializers;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public boolean isDeclared() {
        return isDeclared;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VariableInitializers)) return false;

        VariableInitializers that = (VariableInitializers) o;

        if (isDeclared != that.isDeclared) return false;
        if (isInitialized != that.isInitialized) return false;
        if (possibleLocalInitializers != null
            ? !possibleLocalInitializers.equals(that.possibleLocalInitializers)
            : that.possibleLocalInitializers != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = possibleLocalInitializers != null ? possibleLocalInitializers.hashCode() : 0;
        result = 31 * result + (isInitialized ? 1 : 0);
        result = 31 * result + (isDeclared ? 1 : 0);
        return result;
    }
}
