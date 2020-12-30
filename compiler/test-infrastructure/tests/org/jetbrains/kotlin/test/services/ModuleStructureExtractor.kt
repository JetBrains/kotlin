/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.test.directives.model.DirectivesContainer

abstract class ModuleStructureExtractor(
    protected val testServices: TestServices,
    protected val additionalSourceProviders: List<AdditionalSourceProvider>
) {
    abstract fun splitTestDataByModules(
        testDataFileName: String,
        directivesContainer: DirectivesContainer,
    ): TestModuleStructure
}
