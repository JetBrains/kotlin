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

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleCapability
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.storage.ExceptionTracker
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager

@K1Deprecation
interface GlobalContext {
    val storageManager: StorageManager
    val exceptionTracker: ExceptionTracker
}

@K1Deprecation
interface ProjectContext : GlobalContext {
    val project: Project
}

@K1Deprecation
interface ModuleContext : ProjectContext {
    val module: ModuleDescriptor
}

@K1Deprecation
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

@K1Deprecation
open class SimpleGlobalContext(
    override val storageManager: StorageManager,
    override val exceptionTracker: ExceptionTracker
) : GlobalContext

@K1Deprecation
open class GlobalContextImpl(
    storageManager: LockBasedStorageManager,
    exceptionTracker: ExceptionTracker
) : SimpleGlobalContext(storageManager, exceptionTracker) {
    override val storageManager: LockBasedStorageManager = super.storageManager as LockBasedStorageManager
}

@K1Deprecation
class ProjectContextImpl(
    override val project: Project,
    private val globalContext: GlobalContext
) : ProjectContext, GlobalContext by globalContext

@K1Deprecation
class ModuleContextImpl(
    override val module: ModuleDescriptor,
    projectContext: ProjectContext
) : ModuleContext, ProjectContext by projectContext

@K1Deprecation
class MutableModuleContextImpl(
    override val module: ModuleDescriptorImpl,
    projectContext: ProjectContext
) : MutableModuleContext, ProjectContext by projectContext

@K1Deprecation
fun GlobalContext(debugName: String): GlobalContextImpl {
    val tracker = ExceptionTracker()
    return GlobalContextImpl(LockBasedStorageManager.createWithExceptionHandling(debugName, tracker, {
        ProgressManager.checkCanceled()
    }, { throw ProcessCanceledException(it) }), tracker)
}

@K1Deprecation
fun ProjectContext(project: Project, debugName: String): ProjectContext = ProjectContextImpl(project, GlobalContext(debugName))
@K1Deprecation
fun ModuleContext(module: ModuleDescriptor, project: Project, debugName: String): ModuleContext =
    ModuleContextImpl(module, ProjectContext(project, debugName))

@K1Deprecation
fun GlobalContext.withProject(project: Project): ProjectContext = ProjectContextImpl(project, this)
@K1Deprecation
fun ProjectContext.withModule(module: ModuleDescriptor): ModuleContext = ModuleContextImpl(module, this)

@K1Deprecation
fun ContextForNewModule(
    projectContext: ProjectContext,
    moduleName: Name,
    builtIns: KotlinBuiltIns,
    platform: TargetPlatform?,
    capabilities: Map<ModuleCapability<*>, Any?> = emptyMap(),
): MutableModuleContext {
    val module = ModuleDescriptorImpl(moduleName, projectContext.storageManager, builtIns, platform, capabilities)
    return MutableModuleContextImpl(module, projectContext)
}
