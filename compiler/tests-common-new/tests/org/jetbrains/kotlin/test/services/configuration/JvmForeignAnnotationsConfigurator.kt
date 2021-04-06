/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives.JSPECIFY_STATE
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives.JSR305_GLOBAL_REPORT
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives.JSR305_MIGRATION_REPORT
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives.JSR305_SPECIAL_REPORT
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.utils.JavaTypeEnhancementState
import org.jetbrains.kotlin.utils.ReportLevel

enum class JavaForeignAnnotationType(val path: String) {
    Annotations("third-party/annotations"),
    Java8Annotations("third-party/java8-annotations"),
    Java9Annotations("third-party/java9-annotations");
}

open class JvmForeignAnnotationsConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    companion object {
        const val JSR_305_TEST_ANNOTATIONS_PATH = "compiler/testData/diagnostics/helpers/jsr305_test_annotations"
    }

    override val directivesContainers: List<DirectivesContainer>
        get() = listOf(ForeignAnnotationsDirectives)

    override fun provideAdditionalAnalysisFlags(directives: RegisteredDirectives): Map<AnalysisFlag<*>, Any?> {
        val globalState = directives.singleOrZeroValue(JSR305_GLOBAL_REPORT) ?: ReportLevel.WARN
        val migrationState = directives.singleOrZeroValue(JSR305_MIGRATION_REPORT)
        val userAnnotationsState = directives[JSR305_SPECIAL_REPORT].mapNotNull {
            val (name, stateDescription) = it.split(":").takeIf { it.size == 2 } ?: return@mapNotNull null
            val state = ReportLevel.findByDescription(stateDescription) ?: return@mapNotNull null
            name to state
        }.toMap()
        val jSpecifyReportLevel = directives.singleOrZeroValue(JSPECIFY_STATE) ?: ReportLevel.WARN
        return mapOf(
            JvmAnalysisFlags.javaTypeEnhancementState to JavaTypeEnhancementState(
                globalState,
                migrationState,
                userAnnotationsState,
                jspecifyReportLevel = jSpecifyReportLevel
            )
        )
    }
}
