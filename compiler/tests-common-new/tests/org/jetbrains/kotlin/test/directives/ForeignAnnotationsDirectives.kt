/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.configuration.JavaForeignAnnotationType
import org.jetbrains.kotlin.utils.ReportLevel

@Suppress("RemoveExplicitTypeArguments")
object ForeignAnnotationsDirectives : SimpleDirectivesContainer() {
    val JSR305_GLOBAL_REPORT by enumDirective<ReportLevel>(
        description = "Global report level",
        additionalParser = ReportLevel.Companion::findByDescription
    )

    val JSR305_MIGRATION_REPORT by enumDirective<ReportLevel>(
        description = "Migration report level",
        additionalParser = ReportLevel.Companion::findByDescription
    )

    val JSR305_SPECIAL_REPORT by stringDirective(
        description = "Report level for specific annotations"
    )

    val JSPECIFY_STATE by enumDirective<ReportLevel>(
        description = "Report level for jSpecify annotations",
        additionalParser = ReportLevel.Companion::findByDescription
    )

    val ANNOTATIONS_PATH by enumDirective<JavaForeignAnnotationType>(
        description = "Path to foreign annotations"
    )

    val SOURCE_RETENTION_ANNOTATIONS by directive(
        description = "Skip test against compiled annotation because of their Source retention"
    )

    val MUTE_FOR_PSI_CLASS_FILES_READING by directive(
        description = "Skip test if psi class files reading is used"
    )
}
