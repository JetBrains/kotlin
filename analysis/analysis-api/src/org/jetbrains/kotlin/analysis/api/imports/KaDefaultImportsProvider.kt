/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.imports

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.TargetPlatform

/**
 * Provides a list of default imports for a specific [TargetPlatform].
 */
@KaIdeApi
public interface KaDefaultImportsProvider {
    /**
     * @see org.jetbrains.kotlin.analysis.api.imports.getDefaultImports
     */
    public fun getDefaultImports(targetPlatform: TargetPlatform): KaDefaultImports

    @KaIdeApi
    public companion object {
        public fun getService(project: Project): KaDefaultImportsProvider =
            project.service()
    }
}

/**
 * Provides a list of default imports for a specific [TargetPlatform].
 *
 * This list may vary for each platform.
 * See the [documentation page](https://kotlinlang.org/docs/packages.html#default-imports) for details.
 *
 * Some declarations imported by default are excluded; see [KaDefaultImports.excludedFromDefaultImports].
 */
@KaIdeApi
public fun TargetPlatform.getDefaultImports(project: Project): KaDefaultImports =
    KaDefaultImportsProvider.getService(project).getDefaultImports(this)
