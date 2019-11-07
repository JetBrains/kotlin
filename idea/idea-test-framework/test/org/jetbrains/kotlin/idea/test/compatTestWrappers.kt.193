/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused", "IncompatibleAPI", "PropertyName")

package org.jetbrains.kotlin.idea.test

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightIdeaTestCase

// BUNCH: 192
@Suppress("DEPRECATION")
@Deprecated("Use KotlinLightCodeInsightFixtureTestCase instead")
abstract class KotlinLightCodeInsightTestCase : com.intellij.testFramework.LightCodeInsightTestCase() {
    protected inline val project_: Project get() = project
    protected inline val module_: Module get() = module
    protected inline val editor_: Editor get() = editor
}

// BUNCH: 192
abstract class KotlinLightIdeaTestCase : LightIdeaTestCase() {
    protected inline val project_: Project get() = project
    protected inline val module_: Module get() = module
}