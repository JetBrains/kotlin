/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

class KotlinOverridableInternalMembersShortNameIndex private constructor() : StringStubIndexExtension<KtCallableDeclaration>() {
    override fun getKey() = KEY

    override fun get(name: String, project: Project, scope: GlobalSearchScope): Collection<KtCallableDeclaration> {
        return StubIndex.getElements(KEY, name, project, scope, KtCallableDeclaration::class.java)
    }

    companion object {

        @JvmField
        val Instance = KotlinOverridableInternalMembersShortNameIndex()

        private val KEY = KotlinIndexUtil.createIndexKey(KotlinOverridableInternalMembersShortNameIndex::class.java)
    }
}