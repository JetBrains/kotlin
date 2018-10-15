/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import com.intellij.formatting.ASTBlock
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings

/*
 * ASTBlock is nullable since 182, this extension was introduced to minimize changes between bunches
 */
fun ASTBlock.requireNode() = node ?: error("ASTBlock.getNode() returned null")

/**
 * Can be removed with all usages after moving master to 1.3 with new default code style settings.
 */
val isDefaultOfficialCodeStyle by lazy { !KotlinCodeStyleSettings.DEFAULT.CONTINUATION_INDENT_FOR_CHAINED_CALLS }