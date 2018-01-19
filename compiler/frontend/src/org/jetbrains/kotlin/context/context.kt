/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.context

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.MultiTargetPlatform
import org.jetbrains.kotlin.storage.ExceptionTracker
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager

interface GlobalContext {
    val storageManager: StorageManager
    val exceptionTracker: ExceptionTracker
}

interface ProjectContext : GlobalContext {
    val project: Project
}

interface ModuleContext : ProjectContext {
    val module: ModuleDescriptor
}

interface MutableModuleContext : ModuleContext {
    override val module: ModuleDescriptorImpl

    fun setDependencies(vararg dependencies: ModuleDescriptorImpl) {
        module.setDependencies(*dependencies)
    }

    fun setDependencies(dependencies: List<ModuleDescriptorImpl>) {
        module.setDependencies(dependencies)
    }

    fun initializeModuleContents(packageFragmentProvider: PackageFragmentProvider) {
        module.initialize(packageFragmentProvider)
    }
}

open class SimpleGlobalContext(
    override val storageManager: StorageManager,
    override val exceptionTracker: ExceptionTracker
) : GlobalContext

open class GlobalContextImpl(
    storageManager: LockBasedStorageManager,
    exceptionTracker: ExceptionTracker
) : SimpleGlobalContext(storageManager, exceptionTracker) {
    override val storageManager: LockBasedStorageManager = super.storageManager as LockBasedStorageManager
}

class ProjectContextImpl(
    override val project: Project,
    private val globalContext: GlobalContext
) : ProjectContext, GlobalContext by globalContext

class ModuleContextImpl(
    override val module: ModuleDescriptor,
    projectContext: ProjectContext
) : ModuleContext, ProjectContext by projectContext

class MutableModuleContextImpl(
    override val module: ModuleDescriptorImpl,
    projectContext: ProjectContext
) : MutableModuleContext, ProjectContext by projectContext

fun GlobalContext(): GlobalContextImpl {
    val tracker = ExceptionTracker()
    return GlobalContextImpl(LockBasedStorageManager.createWithExceptionHandling(tracker), tracker)
}

fun ProjectContext(project: Project): ProjectContext = ProjectContextImpl(project, GlobalContext())
fun ModuleContext(module: ModuleDescriptor, project: Project): ModuleContext =
    ModuleContextImpl(module, ProjectContext(project))

fun GlobalContext.withProject(project: Project): ProjectContext = ProjectContextImpl(project, this)
fun ProjectContext.withModule(module: ModuleDescriptor): ModuleContext = ModuleContextImpl(module, this)

fun ContextForNewModule(
    projectContext: ProjectContext,
    moduleName: Name,
    builtIns: KotlinBuiltIns,
    multiTargetPlatform: MultiTargetPlatform?
): MutableModuleContext {
    val module = ModuleDescriptorImpl(moduleName, projectContext.storageManager, builtIns, multiTargetPlatform)
    return MutableModuleContextImpl(module, projectContext)
}
