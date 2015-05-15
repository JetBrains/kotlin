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
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.platform.PlatformToKotlinClassMap
import org.jetbrains.kotlin.storage.ExceptionTracker
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager

public trait GlobalContext {
    public val storageManager: StorageManager
    public val exceptionTracker: ExceptionTracker
}

public trait ProjectContext : GlobalContext {
    public val project: Project
}

public trait ModuleContext : ProjectContext {
    public val module: ModuleDescriptorImpl

    public val platformToKotlinClassMap: PlatformToKotlinClassMap
        get() = module.platformToKotlinClassMap

    public val builtIns: KotlinBuiltIns
        get() = module.builtIns
}

public open class SimpleGlobalContext(
        override val storageManager: StorageManager,
        override val exceptionTracker: ExceptionTracker
) : GlobalContext

public open class GlobalContextImpl(
        storageManager: LockBasedStorageManager,
        exceptionTracker: ExceptionTracker
) : SimpleGlobalContext(storageManager, exceptionTracker) {
    override val storageManager: LockBasedStorageManager = super.storageManager as LockBasedStorageManager
}

public class ProjectContextImpl(
        override val project: Project,
        private val globalContext: GlobalContext
) : ProjectContext, GlobalContext by globalContext

public class ModuleContextImpl(
        override val module: ModuleDescriptorImpl,
        projectContext: ProjectContext
) : ModuleContext, ProjectContext by projectContext

public fun GlobalContext(): GlobalContextImpl {
    val tracker = ExceptionTracker()
    return GlobalContextImpl(LockBasedStorageManager.createWithExceptionHandling(tracker), tracker)
}

public fun ProjectContext(project: Project): ProjectContext = ProjectContextImpl(project, GlobalContext())
public fun ModuleContext(module: ModuleDescriptorImpl, project: Project): ModuleContext =
        ModuleContextImpl(module, ProjectContext(project))

public fun GlobalContext.withProject(project: Project): ProjectContext = ProjectContextImpl(project, this)
public fun ProjectContext.withModule(module: ModuleDescriptorImpl): ModuleContext = ModuleContextImpl(module, this)

deprecated("Used temporarily while we are in transition from to lazy resolve")
public open class TypeLazinessToken {
    deprecated("Used temporarily while we are in transition from to lazy resolve")
    public open fun isLazy(): Boolean = false
}

deprecated("Used temporarily while we are in transition from to lazy resolve")
public class LazyResolveToken : TypeLazinessToken() {
    deprecated("Used temporarily while we are in transition from to lazy resolve")
    override fun isLazy() = true
}
