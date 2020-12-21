/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.directives.model.ComposedRegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestModule

abstract class EnvironmentConfigurator(protected val testServices: TestServices) {
    open val directivesContainers: List<DirectivesContainer>
        get() = emptyList()

    open val additionalServices: List<ServiceRegistrationData>
        get() = emptyList()

    protected val moduleStructure: TestModuleStructure
        get() = testServices.moduleStructure

    protected val TestModule.allRegisteredDirectives: RegisteredDirectives
        get() = ComposedRegisteredDirectives(directives, testServices.defaultDirectives)

    open fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule, project: MockProject) {}
}
