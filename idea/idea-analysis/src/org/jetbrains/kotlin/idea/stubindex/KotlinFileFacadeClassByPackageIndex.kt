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
import org.jetbrains.kotlin.psi.KtFile

class KotlinFileFacadeClassByPackageIndex private constructor() : StringStubIndexExtension<KtFile>() {
    override fun getKey(): StubIndexKey<String, KtFile> = KEY

    override fun get(key: String, project: Project, scope: GlobalSearchScope) =
        StubIndex.getElements(KEY, key, project, scope, KtFile::class.java)

    companion object {
        private val KEY = KotlinIndexUtil.createIndexKey(KotlinFileFacadeClassByPackageIndex::class.java)

        @JvmField
        val INSTANCE: KotlinFileFacadeClassByPackageIndex = KotlinFileFacadeClassByPackageIndex()

        @JvmStatic
        fun getInstance(): KotlinFileFacadeClassByPackageIndex = INSTANCE
    }
}