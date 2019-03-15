/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import org.jetbrains.kotlin.idea.multiplatform.setupMppProjectFromDirStructure
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import java.io.File

abstract class AbstractMultiModuleLineMarkerTest : AbstractMultiModuleHighlightingTest() {

    override fun getTestDataPath() = PluginTestCaseBase.getTestDataPathBase() + "/multiModuleLineMarker/"

    override val shouldCheckLineMarkers = true

    override val shouldCheckResult = false

    override fun doTestLineMarkers() = true

    protected fun doTest(path: String) {
        setupMppProjectFromDirStructure(File(path))
        checkLineMarkersInProject()
    }
}