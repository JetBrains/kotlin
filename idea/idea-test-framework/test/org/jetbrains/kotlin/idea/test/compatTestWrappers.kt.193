/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused", "IncompatibleAPI", "PropertyName")

package org.jetbrains.kotlin.idea.test

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.testFramework.LightIdeaTestCase

// BUNCH: as36, 191
@Suppress("DEPRECATION")
@Deprecated("Use KotlinLightCodeInsightFixtureTestCase instead")
abstract class KotlinLightCodeInsightTestCase : com.intellij.testFramework.LightCodeInsightTestCase() {
    protected inline val project_: Project get() = project
    protected inline val module_: Module get() = module
    protected inline val editor_: Editor get() = editor
    protected inline val file_: PsiFile get() = file
    protected inline val currentEditorDataContext_: DataContext get() = currentEditorDataContext

    protected fun configureFromFileText_(fileName: String, fileText: String): Document {
        return configureFromFileText(fileName, fileText, false)
    }
}

// BUNCH: as36, 191
abstract class KotlinLightIdeaTestCase : LightIdeaTestCase() {
    protected inline val project_: Project get() = project
    protected inline val module_: Module get() = module
}