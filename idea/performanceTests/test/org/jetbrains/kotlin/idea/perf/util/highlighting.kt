/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf.util

import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.EdtTestUtil
import com.intellij.testFramework.ExpectedHighlightingData
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase

fun KotlinLightCodeInsightFixtureTestCase.removeInfoMarkers() {
    ExpectedHighlightingData(editor.document, true, true).init()

    EdtTestUtil.runInEdtAndWait {
        PsiDocumentManager.getInstance(project).commitAllDocuments()
    }
}
