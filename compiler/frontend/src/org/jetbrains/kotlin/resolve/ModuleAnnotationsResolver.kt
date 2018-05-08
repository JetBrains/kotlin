/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.ClassId

interface ModuleAnnotationsResolver {
    fun getAnnotationsOnContainingModule(descriptor: DeclarationDescriptor): List<ClassId>

    companion object {
        fun getInstance(project: Project): ModuleAnnotationsResolver =
            ServiceManager.getService(project, ModuleAnnotationsResolver::class.java)
    }
}
