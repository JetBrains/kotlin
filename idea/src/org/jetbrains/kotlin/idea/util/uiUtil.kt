/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import com.intellij.ui.DocumentAdapter
import javax.swing.event.DocumentEvent
import javax.swing.text.JTextComponent

fun JTextComponent.onTextChange(action: (DocumentEvent) -> Unit) {
    document.addDocumentListener(
            object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    action(e)
                }
            }
    )
}