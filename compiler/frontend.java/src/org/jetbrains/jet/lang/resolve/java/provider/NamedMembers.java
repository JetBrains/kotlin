/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java.provider;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiFieldWrapper;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiMethodWrapper;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.List;

public final class NamedMembers {

    public NamedMembers(@NotNull Name name) {
        this.name = name;
    }

    @NotNull
    private final Name name;

    @NotNull
    private final List<PsiMethodWrapper> methods = Lists.newArrayList();

    @NotNull
    private final List<PsiFieldWrapper> fields = Lists.newArrayList();

    @Nullable
    private PsiClass samInterface;

    void addMethod(@NotNull PsiMethodWrapper method) {
        methods.add(method);
    }

    void addField(@NotNull PsiFieldWrapper field) {
        fields.add(field);
    }

    void setSamInterface(@NotNull PsiClass samInterface) {
        this.samInterface = samInterface;
    }

    @NotNull
    public Name getName() {
        return name;
    }

    @NotNull
    public List<PsiMethodWrapper> getMethods() {
        return methods;
    }

    @NotNull
    public List<PsiFieldWrapper> getFields() {
        return fields;
    }

    @Nullable
    public PsiClass getSamInterface() {
        return samInterface;
    }
}
