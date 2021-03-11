/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

internal interface MultiplatformModelImportingChecker {
    fun check(model: KotlinMPPGradleModel, reportTo: KotlinImportingDiagnosticsContainer, context: MultiplatformModelImportingContext)
}

internal object OrphanSourceSetImportingChecker : MultiplatformModelImportingChecker {
    override fun check(
        model: KotlinMPPGradleModel,
        reportTo: KotlinImportingDiagnosticsContainer,
        context: MultiplatformModelImportingContext
    ) {
        model.sourceSetsByName.values.filter { context.isOrphanSourceSet(it) }
            .mapTo(reportTo) { OrphanSourceSetsImportingDiagnostic(it) }
    }
}