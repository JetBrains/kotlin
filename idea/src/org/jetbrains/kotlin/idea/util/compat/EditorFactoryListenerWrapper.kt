/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("IncompatibleAPI")

package org.jetbrains.kotlin.idea.util.compat

import com.intellij.openapi.editor.event.EditorFactoryListener

// Default implementation for interface methods were added in 183.
// BUNCH: 182
typealias EditorFactoryListenerWrapper = EditorFactoryListener
