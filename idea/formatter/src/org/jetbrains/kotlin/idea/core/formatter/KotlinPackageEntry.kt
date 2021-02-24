/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.formatter

import com.intellij.openapi.application.ApplicationBundle

class KotlinPackageEntry(
    packageName: String,
    val withSubpackages: Boolean
) {
    val packageName = packageName.removeSuffix(".*")

    companion object {
        @JvmField
        val ALL_OTHER_IMPORTS_ENTRY =
            KotlinPackageEntry(ApplicationBundle.message("listbox.import.all.other.imports"), withSubpackages = true)

        @JvmField
        val ALL_OTHER_ALIAS_IMPORTS_ENTRY = KotlinPackageEntry("<all other alias imports>", withSubpackages = true)
    }

    fun matchesPackageName(otherPackageName: String): Boolean {
        if (otherPackageName.startsWith(packageName)) {
            if (otherPackageName.length == packageName.length) return true
            if (withSubpackages) {
                if (otherPackageName[packageName.length] == '.') return true
            }
        }
        return false
    }

    val isSpecial: Boolean get() = this == ALL_OTHER_IMPORTS_ENTRY || this == ALL_OTHER_ALIAS_IMPORTS_ENTRY

    override fun toString(): String {
        return packageName
    }
}