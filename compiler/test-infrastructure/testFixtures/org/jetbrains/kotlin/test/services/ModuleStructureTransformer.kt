/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.test.TestInfrastructureInternals

@TestInfrastructureInternals
abstract class ModuleStructureTransformer {
    abstract fun transformModuleStructure(moduleStructure: TestModuleStructure, defaultsProvider: DefaultsProvider): TestModuleStructure
}

class ExceptionFromModuleStructureTransformer(
    override val cause: Throwable,
    val alreadyParsedModuleStructure: TestModuleStructure
) : Exception(cause)
