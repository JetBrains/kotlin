/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.ui.DialogWrapper

@Suppress("UNUSED_PARAMETER")
internal fun getDataContextFromDialog(wrapper: DialogWrapper): DataContext? = DataManager.getInstance().dataContextFromFocus.result