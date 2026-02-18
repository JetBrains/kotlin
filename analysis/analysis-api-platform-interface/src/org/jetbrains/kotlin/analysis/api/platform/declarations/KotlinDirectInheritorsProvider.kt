/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.declarations

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformComponent
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject

@KaPlatformInterface
public interface KotlinDirectInheritorsProvider : KotlinPlatformComponent {
    /**
     * Returns all direct *Kotlin* inheritors of [ktClass] that can be found in the given [scope].
     *
     * [ktClass] must not be a class from a dangling file, but rather should be a class from a physical source, like a source module.
     * The scope should cover the [ktClass] itself. In case inheritors for a dangling class are needed, [getDirectKotlinInheritors] should
     * be called with the same class from a non-dangling context module. This removes the burden of handling dangling files from the
     * provider, simplifying its implementation.
     *
     * The implementation of [getDirectKotlinInheritors] is allowed to lazy-resolve symbols up to the `SUPER_TYPES` phase. This is required
     * to check subtyping for potential inheritors. Hence, if [getDirectKotlinInheritors] is invoked during lazy resolution, it requires a
     * phase of `SEALED_CLASS_INHERITORS` or later.
     *
     * @param includeLocalInheritors If `false`, only non-local inheritors will be searched and returned.
     */
    public fun getDirectKotlinInheritors(
        ktClass: KtClass,
        scope: GlobalSearchScope,
        includeLocalInheritors: Boolean = true,
    ): Iterable<KtClassOrObject>

    @KaPlatformInterface
    public companion object {
        public fun getInstance(project: Project): KotlinDirectInheritorsProvider = project.service()
    }
}
