/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.stubindex

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import org.jetbrains.kotlin.psi.KtTypeAlias

/**
 * Index of non-top-level type aliases (members of classes)
 * The key is stringified [org.jetbrains.kotlin.name.ClassId] by the rules described in [org.jetbrains.kotlin.name.ClassId.asString]:
 * packages are delimited by '/' and classes by '.', e.g. "kotlin/Map.Entry"
 */
class KotlinInnerTypeAliasClassIdIndex : StringStubIndexExtension<KtTypeAlias>() {
    override fun getKey(): StubIndexKey<String, KtTypeAlias> = KEY

    override fun get(s: String, project: Project, scope: GlobalSearchScope): Collection<KtTypeAlias> =
        StubIndex.getElements(KEY, s, project, scope, KtTypeAlias::class.java)

    companion object {
        val KEY = KotlinIndexUtil.createIndexKey(KotlinInnerTypeAliasClassIdIndex::class.java)
        val INSTANCE = KotlinInnerTypeAliasClassIdIndex()

        @JvmStatic
        fun getInstance() = INSTANCE
    }
}

