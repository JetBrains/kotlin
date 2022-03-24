/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object AnalysisApiTestDirectives : SimpleDirectivesContainer() {
    val DISABLE_DEPENDED_MODE by directive("Analysis in dependent mode should not be run in this test")
}