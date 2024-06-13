/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.declarations

import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.kotlin.analysis.api.platform.KotlinOptionalPlatformComponent
import org.jetbrains.kotlin.psi.KtCodeFragment

public interface KotlinForeignValueProviderService : KotlinOptionalPlatformComponent {
    public fun getForeignValues(codeFragment: KtCodeFragment): Map<String, String>

    public companion object {
        public fun getInstance(): KotlinForeignValueProviderService? {
            return ApplicationManager.getApplication().getService(KotlinForeignValueProviderService::class.java)
        }
    }
}
