/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import com.intellij.psi.impl.compiled.ClsClassImpl

/**
 * Nullable wrapper for qualifiedName.
 * BUNCH: 173
 */
@Suppress("IncompatibleAPI")
val ClsClassImpl.qualifiedNameEx: String? get() = qualifiedName