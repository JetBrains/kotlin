/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.trackers

import com.intellij.psi.impl.PsiModificationTrackerImpl

// BUNCH: 191
@Suppress("unused")
val PsiModificationTrackerImpl.isEnableLanguageTrackerCompat get() = isEnableLanguageTracker