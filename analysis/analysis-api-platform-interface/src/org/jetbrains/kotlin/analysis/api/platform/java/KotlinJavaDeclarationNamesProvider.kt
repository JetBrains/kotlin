/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.java

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

// TODO (marco): Name?
public interface KotlinJavaDeclarationNamesProvider {
    /**
     * Calculates the set of packages contained in the given [module] with at least one **Java-like file**. The set should cover *all*
     * JVM languages (e.g., Scala, Groovy, etc.) *except Kotlin*, or `null` should be returned if files of languages are detected for which
     * package computation is too expensive.
     *
     * The set should be exact (so neither false positives nor false negatives). `null` may be returned if the package set is too expensive
     * or impossible to compute.
     */
    public fun computePackageNames(module: KaModule): Set<String>?

    public companion object {
        public fun getInstance(project: Project): KotlinJavaDeclarationNamesProvider? = project.serviceOrNull()
    }
}
