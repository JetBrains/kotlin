/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import com.intellij.openapi.ui.popup.PopupChooserBuilder
import javax.swing.JList

// BUNCH: 181
@Suppress("IncompatibleAPI")
class PopupChooserBuilderWrapper<T>(list: JList<T>): PopupChooserBuilder<T>(list)