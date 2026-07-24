/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.serialization.deserialization.KotlinMetadataFinder

interface MetadataFinderFactory {
    fun create(scope: GlobalSearchScope): KotlinMetadataFinder
}
