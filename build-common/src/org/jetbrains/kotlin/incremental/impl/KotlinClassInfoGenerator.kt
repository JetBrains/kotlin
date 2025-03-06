/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.impl

import org.jetbrains.kotlin.incremental.KotlinClassInfo
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

/**
 * We need to provide the normal behavior for compatibility with pre-depgraph JPS,
 * but we also need to allow configurable behavior for Gradle Classpath Snapshotting transformations
 */
object KotlinClassInfoGenerator {
}
