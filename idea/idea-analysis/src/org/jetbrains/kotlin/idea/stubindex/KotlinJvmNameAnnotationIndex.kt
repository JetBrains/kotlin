/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.stubindex

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import org.jetbrains.kotlin.psi.KtAnnotationEntry

class KotlinJvmNameAnnotationIndex private constructor() : StringStubIndexExtension<KtAnnotationEntry>() {
    override fun getKey(): StubIndexKey<String, KtAnnotationEntry> = KEY

    override fun get(key: String, project: Project, scope: GlobalSearchScope): Collection<KtAnnotationEntry> =
        StubIndex.getElements(KEY, key, project, scope, KtAnnotationEntry::class.java)

    companion object {
        private val KEY = KotlinIndexUtil.createIndexKey(KotlinJvmNameAnnotationIndex::class.java)

        val INSTANCE: KotlinJvmNameAnnotationIndex = KotlinJvmNameAnnotationIndex()

        @JvmStatic
        fun getInstance(): KotlinJvmNameAnnotationIndex = INSTANCE
    }
}