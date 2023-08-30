/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test.cases.session.builder

import java.nio.file.Path
import java.nio.file.Paths

internal fun testDataPath(path: String): Path {
    return Paths.get("analysis/analysis-api-standalone/testData/sessionBuilder").resolve(path)
}