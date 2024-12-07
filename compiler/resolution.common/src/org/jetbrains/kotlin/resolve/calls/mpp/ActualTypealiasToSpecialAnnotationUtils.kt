/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.mpp

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.ClassId

object ActualTypealiasToSpecialAnnotationUtils {
    /**
     * Type aliases may be useful to commonize some platform-specific annotations, e.g. [kotlin.jvm.Synchronized].
     * We cannot universally determine at compile time whether annotation is accessible in common code.
     * Instead, we prohibit certain package names because we are sure that annotations from them
     * are visible in common and there is no point to typealias them.
     */
    private val FORBIDDEN_PACKAGES = setOf(
        StandardNames.ANNOTATION_PACKAGE_FQ_NAME,
        StandardNames.BUILT_INS_PACKAGE_FQ_NAME,
        StandardNames.KOTLIN_INTERNAL_FQ_NAME,
    )

    fun isAnnotationProhibitedInActualTypeAlias(classId: ClassId): Boolean {
        return classId.packageFqName in FORBIDDEN_PACKAGES
    }
}
