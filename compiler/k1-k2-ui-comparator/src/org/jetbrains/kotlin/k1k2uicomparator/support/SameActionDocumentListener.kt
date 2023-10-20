/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.k1k2uicomparator.support

import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

fun interface SameActionDocumentListener : DocumentListener {
    fun action(e: DocumentEvent?)

    override fun insertUpdate(e: DocumentEvent?) = action(e)
    override fun removeUpdate(e: DocumentEvent?) = action(e)
    override fun changedUpdate(e: DocumentEvent?) = action(e)
}
