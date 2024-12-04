/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.testfixtures

import org.jetbrains.kotlin.daemon.common.CompilerServicesFacadeBase
import org.jetbrains.kotlin.daemon.common.ReportCategory
import org.jetbrains.kotlin.daemon.common.ReportSeverity
import java.io.Serializable

class FakeCompilerServicesFacadeBase : CompilerServicesFacadeBase {

    val messages: Map<String, Pair<ReportCategory, ReportSeverity>>
        get() = messageMap.toMap()

    private val messageMap = mutableMapOf<String, Pair<ReportCategory, ReportSeverity>>()

    override fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) {
        messageMap[message!!] = ReportCategory.fromCode(category)!! to ReportSeverity.fromCode(severity)
    }
}
