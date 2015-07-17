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

package org.jetbrains.kotlin.idea.versions;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KotlinRuntimeLibraryCoreUtil {
    private static final ImmutableList<String> CANDIDATE_CLASSES = ImmutableList.of(
            "kotlin.Unit",

            // For older versions
            "jet.runtime.Intrinsics"
    );

    @Nullable
    public static PsiClass getKotlinRuntimeMarkerClass(@NotNull final Project project, @NotNull final GlobalSearchScope scope) {
        return ApplicationManager.getApplication().runReadAction(new Computable<PsiClass>() {
            @Override
            public PsiClass compute() {
                for (String className : CANDIDATE_CLASSES) {
                    PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(className, scope);
                    if (psiClass != null) {
                        return psiClass;
                    }
                }
                return null;
            }
        });
    }
}
