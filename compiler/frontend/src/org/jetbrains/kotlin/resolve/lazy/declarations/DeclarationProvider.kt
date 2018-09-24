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

package org.jetbrains.kotlin.resolve.lazy.declarations

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.lazy.data.KtClassLikeInfo
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter

interface DeclarationProvider {
    fun getDeclarations(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): List<KtDeclaration>

    fun getFunctionDeclarations(name: Name): Collection<KtNamedFunction>

    fun getPropertyDeclarations(name: Name): Collection<KtProperty>

    fun getDestructuringDeclarationsEntries(name: Name): Collection<KtDestructuringDeclarationEntry>

    fun getClassOrObjectDeclarations(name: Name): Collection<KtClassLikeInfo>

    fun getTypeAliasDeclarations(name: Name): Collection<KtTypeAlias>

    fun getDeclarationNames(): Set<Name>
}
