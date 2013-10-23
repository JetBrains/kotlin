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

package org.jetbrains.jet.lang.resolve;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

public interface Diagnostics extends Iterable<Diagnostic> {
    @NotNull
    Collection<Diagnostic> all();

    @NotNull
    Collection<Diagnostic> forElement(@NotNull PsiElement psiElement);

    boolean isEmpty();

    @NotNull
    Diagnostics noSuppression();

    Diagnostics EMPTY = new Diagnostics() {
        @NotNull
        @Override
        public Collection<Diagnostic> all() {
            return Collections.emptyList();
        }

        @NotNull
        @Override
        public Collection<Diagnostic> forElement(@NotNull PsiElement psiElement) {
            return Collections.emptyList();
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @NotNull
        @Override
        public Diagnostics noSuppression() {
            return this;
        }

        @NotNull
        @Override
        public Iterator<Diagnostic> iterator() {
            return all().iterator();
        }
    };
}
