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

package org.jetbrains.kotlin.psi;

import com.intellij.psi.PsiNameIdentifierOwner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;

/**
 * Represents a {@link KtDeclaration} that introduces a name, such as a class, function, property, or type alias.
 *
 * <p>A named declaration exposes its name both as a nullable {@link Name} (via {@link KtNamed#getNameAsName()}) and, for
 * convenience, as a non-null {@link #getNameAsSafeName() safe name}. It also participates in the platform's
 * name-identifier machinery through {@link PsiNameIdentifierOwner}, so its {@link #getNameIdentifier() identifier token}
 * can be located for navigation and refactoring.
 */
public interface KtNamedDeclaration extends KtDeclaration, PsiNameIdentifierOwner, KtStatementExpression, KtNamed {
    /**
     * Returns the name of this declaration, or a special "no name provided" {@link Name} when the declaration is
     * anonymous or its name is missing. Unlike {@link KtNamed#getNameAsName()}, this method never returns {@code null}.
     */
    @NotNull
    Name getNameAsSafeName();

    /**
     * Returns the fully qualified name of this declaration, or {@code null} if it cannot be determined (for example,
     * for local declarations or an anonymous declaration).
     */
    @Nullable
    FqName getFqName();
}
