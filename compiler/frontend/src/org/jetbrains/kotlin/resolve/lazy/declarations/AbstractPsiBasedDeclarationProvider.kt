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

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Sets
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.lazy.ResolveSessionUtils.safeNameForLazyResolve
import org.jetbrains.kotlin.resolve.lazy.data.KtClassInfoUtil
import org.jetbrains.kotlin.resolve.lazy.data.KtClassLikeInfo
import org.jetbrains.kotlin.resolve.lazy.data.KtScriptInfo
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.storage.StorageManager
import java.util.*

abstract class AbstractPsiBasedDeclarationProvider(storageManager: StorageManager) : DeclarationProvider {

    protected class Index {
        // This mutable state is only modified under inside the computable
        val allDeclarations = ArrayList<KtDeclaration>()
        val functions = ArrayListMultimap.create<Name, KtNamedFunction>()
        val properties = ArrayListMultimap.create<Name, KtProperty>()
        val classesAndObjects = ArrayListMultimap.create<Name, KtClassLikeInfo>() // order matters here
        val typeAliases = ArrayListMultimap.create<Name, KtTypeAlias>()
        val destructuringDeclarationsEntries = ArrayListMultimap.create<Name, KtDestructuringDeclarationEntry>()
        val names = Sets.newHashSet<Name>()

        fun putToIndex(declaration: KtDeclaration) {
            if (declaration is KtAnonymousInitializer || declaration is KtSecondaryConstructor) return

            allDeclarations.add(declaration)
            when (declaration) {
                is KtNamedFunction ->
                    functions.put(safeNameForLazyResolve(declaration), declaration)
                is KtProperty ->
                    properties.put(safeNameForLazyResolve(declaration), declaration)
                is KtTypeAlias ->
                    typeAliases.put(safeNameForLazyResolve(declaration.nameAsName), declaration)
                is KtClassOrObject ->
                    classesAndObjects.put(safeNameForLazyResolve(declaration.nameAsName), KtClassInfoUtil.createClassLikeInfo(declaration))
                is KtScript -> {
                    val scriptInfo = KtScriptInfo(declaration)
                    classesAndObjects.put(scriptInfo.script.nameAsName, scriptInfo)
                }
                is KtDestructuringDeclaration -> {
                    for (entry in declaration.entries) {
                        val name = safeNameForLazyResolve(entry.nameAsName)
                        destructuringDeclarationsEntries.put(name, entry)
                        names.add(name)
                    }
                }
                is KtParameter -> {
                    // Do nothing, just put it into allDeclarations is enough
                }
                else -> throw IllegalArgumentException("Unknown declaration: " + declaration)
            }

            when (declaration) {
                is KtNamedDeclaration -> names.add(safeNameForLazyResolve(declaration))
            }
        }

        override fun toString() = "allDeclarations: " + allDeclarations.mapNotNull { it.name }
    }

    override fun getDeclarationNames() = index().names

    private val index = storageManager.createLazyValue<Index> {
        val index = Index()
        doCreateIndex(index)
        index
    }

    protected abstract fun doCreateIndex(index: Index)

    internal fun toInfoString() = toString() + ": " + index().toString()

    override fun getDeclarations(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): List<KtDeclaration>
            = index().allDeclarations

    override fun getFunctionDeclarations(name: Name): List<KtNamedFunction>
            = index().functions[safeNameForLazyResolve(name)].toList()

    override fun getPropertyDeclarations(name: Name): List<KtProperty>
            = index().properties[safeNameForLazyResolve(name)].toList()

    override fun getDestructuringDeclarationsEntries(name: Name): Collection<KtDestructuringDeclarationEntry>
            = index().destructuringDeclarationsEntries[safeNameForLazyResolve(name)].toList()

    override fun getClassOrObjectDeclarations(name: Name): Collection<KtClassLikeInfo>
            = index().classesAndObjects[safeNameForLazyResolve(name)]

    override fun getTypeAliasDeclarations(name: Name): Collection<KtTypeAlias>
            = index().typeAliases[safeNameForLazyResolve(name)]
}
