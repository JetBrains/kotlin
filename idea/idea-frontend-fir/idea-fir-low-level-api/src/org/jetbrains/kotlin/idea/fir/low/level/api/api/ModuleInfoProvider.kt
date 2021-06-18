/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.api

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.psi.KtElement

abstract class ModuleInfoProvider {

    companion object {
        fun getInstance(project: Project): ModuleInfoProvider = ServiceManager.getService(project, ModuleInfoProvider::class.java)
    }
}

