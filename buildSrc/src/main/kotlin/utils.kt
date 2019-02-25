/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("UltimateBuildSrc")
@file:JvmMultifileClass

package org.jetbrains.kotlin.ultimate

import org.gradle.api.Action
import org.gradle.api.file.CopySourceSpec
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.AbstractCopyTask
import java.io.File

// an adapter to use `from()` in pure Kotlin code in the same way as it is used in *.gradle.kts
internal fun AbstractCopyTask.from(
        sourcePath: Any?,
        configureAction: CopySpec.() -> Unit
): CopySourceSpec = from(sourcePath as Any, Action { configureAction() })

// property-like access to Gradle `Property` instance
var Property<String>.value
    get() = get()
    set(value) = set(value)

var DirectoryProperty.value: File
    get() = get().asFile
    set(value) = set(value)
