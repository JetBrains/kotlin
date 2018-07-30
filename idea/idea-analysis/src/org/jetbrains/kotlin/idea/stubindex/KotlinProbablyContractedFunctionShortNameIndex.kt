/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.stubindex

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import org.jetbrains.kotlin.psi.KtNamedFunction

class KotlinProbablyContractedFunctionShortNameIndex : StringStubIndexExtension<KtNamedFunction>() {
    override fun getKey(): StubIndexKey<String, KtNamedFunction> = KEY

    override fun get(name: String, project: Project, scope: GlobalSearchScope): MutableCollection<KtNamedFunction> =
        StubIndex.getElements(KEY, name, project, scope, KtNamedFunction::class.java)

    companion object {
        private val KEY: StubIndexKey<String, KtNamedFunction> =
            KotlinIndexUtil.createIndexKey(KotlinProbablyContractedFunctionShortNameIndex::class.java)

        private val ourInstance = KotlinProbablyContractedFunctionShortNameIndex()

        @JvmStatic
        fun getInstance(): KotlinProbablyContractedFunctionShortNameIndex = ourInstance
    }
}