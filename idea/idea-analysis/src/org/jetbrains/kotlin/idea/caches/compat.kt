/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches

import com.intellij.psi.PsiMethod
import com.intellij.util.Processor

// FIX ME WHEN BUNCH 193 REMOVED
typealias StringProcessor = Processor<in String>
typealias PsiMethodProcessor = Processor<in PsiMethod>

