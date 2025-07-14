/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

abstract class PreAnalysisHandler(protected val testServices: TestServices) {
    abstract fun preprocessModuleStructure(moduleStructure: TestModuleStructure)

    open fun prepareSealedClassInheritors(moduleStructure: TestModuleStructure) {}
}
