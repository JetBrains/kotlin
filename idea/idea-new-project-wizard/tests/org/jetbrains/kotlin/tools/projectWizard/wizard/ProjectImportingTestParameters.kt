/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard

import org.jetbrains.kotlin.tools.projectWizard.cli.TestParameters
import java.nio.file.Path

data class ProjectImportingTestParameters(val runForMaven: Boolean = false) : TestParameters {
    companion object {
        fun fromTestDataOrDefault(directory: Path): ProjectImportingTestParameters =
            TestParameters.fromTestDataOrDefault(directory, PARAMETERS_FILE_NAME)

        private const val PARAMETERS_FILE_NAME = "importParameters.txt"
    }
}