/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClass

/**
 * [KotlinSealedInheritorsProvider] provides a list of direct inheritors for sealed [KtClass]es (which may be a class or an interface). It
 * may be created with [KotlinSealedInheritorsProviderFactory].
 *
 * Implementations of this service should consider caching the results. The [KotlinSealedInheritorsProvider] is tied to the lifetime of its
 * owning session, so it will be invalidated automatically with the session.
 */
public interface KotlinSealedInheritorsProvider {
    /**
     * Returns the sealed inheritors of [ktClass], which may be a class or an interface. [ktClass] is guaranteed to be sealed.
     */
    public fun getSealedInheritors(ktClass: KtClass): List<ClassId>
}

public interface KotlinSealedInheritorsProviderFactory {
    /**
     * Creates a [KotlinSealedInheritorsProvider] for a session.
     */
    public fun createSealedInheritorsProvider(): KotlinSealedInheritorsProvider

    public companion object {
        public fun getInstance(project: Project): KotlinSealedInheritorsProviderFactory? =
            project.getService(KotlinSealedInheritorsProviderFactory::class.java)
    }
}
