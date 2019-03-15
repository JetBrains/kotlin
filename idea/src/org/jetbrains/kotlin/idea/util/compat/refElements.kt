/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util.compat

import com.intellij.codeInspection.reference.RefFile
import com.intellij.psi.PsiFile

// BUNCH: 182
@Suppress("IncompatibleAPI", "MissingRecentApi")
val RefFile.psiFile: PsiFile?
    get() = psiElement