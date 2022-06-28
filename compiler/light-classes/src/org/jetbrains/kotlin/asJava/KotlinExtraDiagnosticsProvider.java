/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics;

public interface KotlinExtraDiagnosticsProvider {
    @NotNull
    Diagnostics forClassOrObject(@NotNull KtClassOrObject kclass);

    @NotNull
    Diagnostics forFacade(@NotNull KtFile file, @NotNull GlobalSearchScope moduleScope);

    static @NotNull KotlinExtraDiagnosticsProvider getInstance(@NotNull Project project) {
        return project.getService(KotlinExtraDiagnosticsProvider.class);
    }
}