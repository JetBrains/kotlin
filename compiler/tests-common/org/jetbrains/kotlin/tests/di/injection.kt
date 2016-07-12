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

package org.jetbrains.kotlin.tests.di

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.LanguageFeatureSettings
import org.jetbrains.kotlin.container.*
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.frontend.di.configureModule
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.resolve.DescriptorResolver
import org.jetbrains.kotlin.resolve.FunctionDescriptorResolver
import org.jetbrains.kotlin.resolve.TypeResolver
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices
import org.jetbrains.kotlin.types.expressions.FakeCallResolver

fun createContainerForTests(project: Project, module: ModuleDescriptor): ContainerForTests {
    return ContainerForTests(createContainer("Tests") {
        configureModule(ModuleContext(module, project), JvmPlatform)
        useInstance(LookupTracker.DO_NOTHING)
        useInstance(LanguageFeatureSettings.LATEST)
        useImpl<ExpressionTypingServices>()
    })
}

class ContainerForTests(container: StorageComponentContainer) {
    val descriptorResolver: DescriptorResolver by container
    val functionDescriptorResolver: FunctionDescriptorResolver by container
    val typeResolver: TypeResolver by container
    val fakeCallResolver: FakeCallResolver by container
    val expressionTypingServices: ExpressionTypingServices by container
}
