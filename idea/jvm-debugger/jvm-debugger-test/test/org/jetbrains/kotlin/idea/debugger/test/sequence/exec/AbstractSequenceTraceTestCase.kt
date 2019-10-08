/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.test.sequence.exec

import com.intellij.debugger.streams.lib.LibrarySupportProvider
import org.jetbrains.kotlin.idea.debugger.sequence.lib.sequence.KotlinSequenceSupportProvider

abstract class AbstractSequenceTraceTestCase : KotlinTraceTestCase() {
    override val librarySupportProvider: LibrarySupportProvider = KotlinSequenceSupportProvider()
}