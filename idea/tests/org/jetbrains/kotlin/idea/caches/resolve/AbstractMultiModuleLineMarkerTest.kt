/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import org.jetbrains.kotlin.idea.codeMetaInfo.AbstractLineMarkerCodeMetaInfoTest
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase

abstract class AbstractMultiModuleLineMarkerTest : AbstractLineMarkerCodeMetaInfoTest() {

    override fun getTestDataPath() = PluginTestCaseBase.getTestDataPathBase() + "/multiModuleLineMarker/"
}