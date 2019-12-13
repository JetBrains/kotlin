/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.stubindex

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import org.jetbrains.kotlin.psi.KtCallableDeclaration

class KotlinTopLevelExtensionsByReceiverTypeIndex private constructor() : StringStubIndexExtension<KtCallableDeclaration>() {

    override fun getKey() = KEY

    override fun get(s: String, project: Project, scope: GlobalSearchScope) =
        StubIndex.getElements(KEY, s, project, scope, KtCallableDeclaration::class.java)

    companion object {
        private val KEY =
            KotlinIndexUtil.createIndexKey<String, KtCallableDeclaration>(KotlinTopLevelExtensionsByReceiverTypeIndex::class.java)
        private const val SEPARATOR = '\n'

        val INSTANCE: KotlinTopLevelExtensionsByReceiverTypeIndex = KotlinTopLevelExtensionsByReceiverTypeIndex()

        fun buildKey(receiverTypeName: String, callableName: String): String = receiverTypeName + SEPARATOR + callableName

        fun receiverTypeNameFromKey(key: String): String = key.substringBefore(SEPARATOR, "")

        fun callableNameFromKey(key: String): String = key.substringAfter(SEPARATOR, "")
    }
}
