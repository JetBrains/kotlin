/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.declarations

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformComponent
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

// TODO: Better name.
public interface KotlinJvmClassIdProvider {
    public fun getTopLevelClassIdsByShortName(shortName: Name): Set<ClassId>?
}

public interface KotlinJvmClassIdProviderFactory : KotlinPlatformComponent {
    public fun createProvider(scope: GlobalSearchScope): KotlinJvmClassIdProvider

    public companion object {
        public fun getInstance(project: Project): KotlinJvmClassIdProviderFactory = project.service()
    }
}
