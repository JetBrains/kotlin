/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services

import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinForeignValueProviderService
import org.jetbrains.kotlin.psi.KtCodeFragment

class TestForeignValue(val name: String, val internalType: String)

class TestForeignValueProviderService : KotlinForeignValueProviderService {
    override fun getForeignValues(codeFragment: KtCodeFragment): Map<String, String> {
        return codeFragment.getUserData(FOREIGN_VALUES_KEY) ?: emptyMap()
    }

    companion object {
        private val FOREIGN_VALUES_KEY = Key<Map<String, String>>("TestForeignValues")

        fun submitForeignValues(codeFragment: KtCodeFragment, values: List<TestForeignValue>) {
            require(codeFragment.getUserData(FOREIGN_VALUES_KEY) == null)

            val map = values.map { Pair(it.name, it.internalType) }.toMap()
            codeFragment.putUserData(FOREIGN_VALUES_KEY, map)
        }
    }
}