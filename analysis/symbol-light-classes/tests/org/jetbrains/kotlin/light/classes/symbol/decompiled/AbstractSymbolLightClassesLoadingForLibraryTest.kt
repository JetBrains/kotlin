/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.decompiled

import org.jetbrains.kotlin.light.classes.symbol.base.AbstractSymbolLightClassesLoadingTest
import org.jetbrains.kotlin.light.classes.symbol.decompiled.service.SymbolLightClassesForLibraryFrontendApiTestConfiguratorService
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

abstract class AbstractSymbolLightClassesLoadingForLibraryTest :
    AbstractSymbolLightClassesLoadingTest(
        SymbolLightClassesForLibraryFrontendApiTestConfiguratorService,
        EXTENSIONS.LIB_JAVA,
        stopIfCompilationErrorDirectivePresent = true
    )
