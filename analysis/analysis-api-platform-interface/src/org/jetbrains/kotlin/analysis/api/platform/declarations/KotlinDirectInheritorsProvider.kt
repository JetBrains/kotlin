/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.declarations

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformComponent
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject

public interface KotlinDirectInheritorsProvider : KotlinPlatformComponent {
    /**
     * Returns all direct *Kotlin* inheritors of [ktClass] that can be found in the given [scope].
     *
     * The implementor of [getDirectKotlinInheritors] is allowed to lazy-resolve symbols up to the `SUPER_TYPES` phase. This is required to
     * check subtyping for potential inheritors. Hence, if [getDirectKotlinInheritors] is invoked during lazy resolution, it requires a
     * phase of `SEALED_CLASS_INHERITORS` or later.
     *
     * @param includeLocalInheritors If `false`, only non-local inheritors will be searched and returned.
     */
    public fun getDirectKotlinInheritors(
        ktClass: KtClass,
        scope: GlobalSearchScope,
        includeLocalInheritors: Boolean = true,
    ): Iterable<KtClassOrObject>

    public companion object {
        public fun getInstance(project: Project): KotlinDirectInheritorsProvider = project.service()
    }
}
