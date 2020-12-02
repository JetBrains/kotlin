/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.classic

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.model.TestModule

class ModuleDescriptorProvider(
    private val testServices: TestServices
) : TestService {
    private val moduleDescriptorByModule = mutableMapOf<TestModule, ModuleDescriptorImpl>()

    fun getModuleDescriptor(testModule: TestModule): ModuleDescriptorImpl {
        return moduleDescriptorByModule[testModule] ?: testServices.assertions.fail {
            "Module descriptor for module ${testModule.name} not found"
        }
    }

    fun replaceModuleDescriptorForModule(testModule: TestModule, moduleDescriptor: ModuleDescriptor) {
        require(moduleDescriptor is ModuleDescriptorImpl)
        moduleDescriptorByModule[testModule] = moduleDescriptor
    }
}

val TestServices.moduleDescriptorProvider: ModuleDescriptorProvider by TestServices.testServiceAccessor()
