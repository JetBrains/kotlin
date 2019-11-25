/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.idea.scratch.output

import com.intellij.openapi.editor.InlayModel

// BUNCH: 183
fun InlayModel.addInlay(offset: Int, renderer: InlayScratchFileRenderer) {
    addAfterLineEndElement(offset, false, renderer)
}

// BUNCH: 183
fun InlayModel.getInlays(start: Int, end: Int) =
    getAfterLineEndElementsInRange(start, end).filter { it.renderer is InlayScratchFileRenderer }