/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.stubindex

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import org.jetbrains.kotlin.psi.KtCallableDeclaration

class KotlinTopLevelExtensionsByReceiverTypeIndex private constructor() : StringStubIndexExtension<KtCallableDeclaration>() {

    override fun getKey() = KEY

    override fun get(s: String, project: Project, scope: GlobalSearchScope)
            = StubIndex.getElements(KEY, s, project, scope, KtCallableDeclaration::class.java)

    companion object {
        private val KEY = KotlinIndexUtil.createIndexKey<String, KtCallableDeclaration>(KotlinTopLevelExtensionsByReceiverTypeIndex::class.java)
        private val SEPARATOR = '\n'

        val INSTANCE: KotlinTopLevelExtensionsByReceiverTypeIndex = KotlinTopLevelExtensionsByReceiverTypeIndex()

        fun buildKey(receiverTypeName: String, callableName: String): String = receiverTypeName + SEPARATOR + callableName

        fun receiverTypeNameFromKey(key: String): String = key.substringBefore(SEPARATOR, "")

        fun callableNameFromKey(key: String): String = key.substringAfter(SEPARATOR, "")
    }
}
