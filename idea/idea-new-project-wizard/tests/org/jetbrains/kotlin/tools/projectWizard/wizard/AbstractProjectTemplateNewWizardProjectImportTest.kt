/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard

import org.jetbrains.kotlin.tools.projectWizard.cli.BuildSystem
import org.jetbrains.kotlin.tools.projectWizard.cli.ProjectTemplateBasedTestWizard
import java.nio.file.Path

abstract class AbstractProjectTemplateNewWizardProjectImportTest : AbstractNewWizardProjectImportTest() {
    override fun createWizard(directory: Path, buildSystem: BuildSystem, projectDirectory: Path): Wizard =
        ProjectTemplateBasedTestWizard.createByDirectory(directory, buildSystem, projectDirectory, IDE_WIZARD_TEST_SERVICES_MANAGER)
}