/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * A minimal interface that {@link KtElement} implements for the purpose of code-generation that does not need the full power of PSI.
 * This interface can be easily implemented by synthetic elements to generate code for them.
 */
public interface KtPureElement {
    /**
     * Returns this or parent source element (for synthetic element declarations).
     * Use it only for the purposes of source attribution.
     */
    @NotNull
    KtElement getPsiOrParent();

    /**
     * Returns parent source element.
     */
    @NotNull
    PsiElement getParent();

    @NotNull
    KtFile getContainingKtFile();
}
