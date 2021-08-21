/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers

import com.intellij.openapi.components.ServiceManager
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.psi.KtElement

public abstract class KotlinModuleInfoProvider {
    public abstract fun getModuleInfo(element: KtElement): ModuleInfo
}

public fun KtElement.getModuleInfo(): ModuleInfo =
    ServiceManager.getService(project, KotlinModuleInfoProvider::class.java).getModuleInfo(this)