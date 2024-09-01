/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.builtins

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.resolve.lazy.createResolveSessionForFiles
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.util.regex.Pattern

object BuiltinsTestUtils {
    fun compileBuiltinsModule(environment: KotlinCoreEnvironment): ModuleDescriptor {
        val files = KotlinTestUtils.loadToKtFiles(
            environment, ContainerUtil.concat<File>(
                allFilesUnder("libraries/stdlib/jvm/builtins"),
                allFilesUnder("core/builtins/build/src/common"),
                allFilesUnder("core/builtins/build/src/reflect"),
            )
        )
        return createResolveSessionForFiles(environment.project, files, false).moduleDescriptor
    }

    @JvmField
    val BUILTIN_PACKAGE_NAMES = listOf(
        StandardNames.BUILT_INS_PACKAGE_FQ_NAME,
        StandardNames.COLLECTIONS_PACKAGE_FQ_NAME,
        StandardNames.RANGES_PACKAGE_FQ_NAME
    )

    private fun allFilesUnder(directory: String): List<File?> {
        return FileUtil.findFilesByMask(Pattern.compile(".*\\.kt"), File(directory))
    }
}
