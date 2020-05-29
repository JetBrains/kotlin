/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.changeSignature.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.Messages.OkCancelResult
import org.jetbrains.kotlin.descriptors.Visibility
import javax.swing.Icon

// FIX ME WHEN BUNCH 193 REMOVED

@OkCancelResult
fun showOkCancelDialog(
    project: Project?,
    message: String?,
    title: String,
    icon: Icon?
): Int {
    return Messages.showOkCancelDialog(
        project,
        message,
        title,
        icon
    )
}

typealias ComboBox = javax.swing.JComboBox<Visibility>
