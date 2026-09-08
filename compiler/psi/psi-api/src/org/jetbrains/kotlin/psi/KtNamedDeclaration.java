/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.psi.PsiNameIdentifierOwner;
import kotlin.SubclassOptInRequired;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;

/**
 * Represents a {@link KtDeclaration} that introduces a name, such as a class, function, property, or type alias.
 *
 * <p>A named declaration exposes its name both as a nullable {@link Name} (via {@link KtNamed#getNameAsName()}) and, for convenience, as a
 * non-null {@link #getNameAsSafeName() safe name}. It also participates in the platform's name-identifier machinery through
 * {@link PsiNameIdentifierOwner}, so its {@link #getNameIdentifier() identifier token} can be located for navigation and refactoring.
 */
@SubclassOptInRequired(markerClass = KtImplementationDetail.class)
public interface KtNamedDeclaration extends KtDeclaration, PsiNameIdentifierOwner, KtStatementExpression, KtNamed {
    /**
     * Returns the name of this declaration, or a special "no name provided" {@link Name} when the declaration is anonymous or its name is
     * missing. Unlike {@link KtNamed#getNameAsName()}, this method never returns {@code null}.
     */
    @NotNull
    Name getNameAsSafeName();

    /**
     * Returns the fully qualified name of this declaration, or {@code null} if it cannot be determined (for example, for local declarations
     * or an anonymous declaration).
     */
    @Nullable
    FqName getFqName();
}
