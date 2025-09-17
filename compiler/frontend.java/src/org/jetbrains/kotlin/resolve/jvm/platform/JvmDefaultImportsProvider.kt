/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.platform

import org.jetbrains.kotlin.builtins.jvm.JvmBuiltIns
import org.jetbrains.kotlin.resolve.DefaultImportProvider
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.StorageManager

object JvmDefaultImportsProvider : DefaultImportProvider() {
    override fun computePlatformSpecificDefaultImports(storageManager: StorageManager, result: MutableList<ImportPath>) {
        result.add(ImportPath.Companion.fromString("kotlin.jvm.*"))

        fun addAllClassifiersFromScope(scope: MemberScope) {
            for (descriptor in scope.getContributedDescriptors(DescriptorKindFilter.Companion.CLASSIFIERS, MemberScope.Companion.ALL_NAME_FILTER)) {
                result.add(ImportPath(DescriptorUtils.getFqNameSafe(descriptor), false))
            }
        }

        for (builtInPackage in JvmBuiltIns(storageManager, JvmBuiltIns.Kind.FALLBACK).builtInPackagesImportedByDefault) {
            addAllClassifiersFromScope(builtInPackage.memberScope)
        }
    }

    override val defaultLowPriorityImports: List<ImportPath> = listOf(ImportPath.Companion.fromString("java.lang.*"))
}
