/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.ClassId

// This interface was introduced to load module annotations for Kotlin/JVM to support the -Xexperimental mode, where the whole module
// would be marked as experimental, and usages of any declarations from the module would require an explicit opt-in.
// It was decided to remove this feature shortly thereafter, so now this interface is unused.
// It can be useful again in the future if there are going to be other use cases for module annotations.
interface ModuleAnnotationsResolver {
    fun getAnnotationsOnContainingModule(descriptor: DeclarationDescriptor): List<ClassId>

    companion object {
        fun getInstance(project: Project): ModuleAnnotationsResolver =
            project.getService(ModuleAnnotationsResolver::class.java)
    }
}
