/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.psiUtil

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.psi.KtFile

/**
 * Partial copy of org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
 */
internal object PackagePartClassUtils {
    private const val PART_CLASS_NAME_SUFFIX = "Kt"

    @JvmStatic
    fun getPackagePartFqName(packageFqName: FqName, file: KtFile): FqName {
        val partClassName = getFilePartShortName(file)
        return packageFqName.child(Name.identifier(partClassName))
    }

    @JvmStatic
    fun getFilePartShortName(file: KtFile): String {
        val fileName = file.name
        val nameWithoutExtension = FileUtil.getNameWithoutExtension(fileName)

        // Decompiled .class files already have `Kt` suffix
        if (file.isCompiled) {
            return nameWithoutExtension
        }

        return NameUtils.getPackagePartClassNamePrefix(nameWithoutExtension) + PART_CLASS_NAME_SUFFIX
    }
}
