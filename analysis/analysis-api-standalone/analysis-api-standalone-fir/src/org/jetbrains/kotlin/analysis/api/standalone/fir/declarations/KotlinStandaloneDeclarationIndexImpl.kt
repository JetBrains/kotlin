/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.declarations

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

internal class KotlinStandaloneDeclarationIndexImpl : KotlinStandaloneDeclarationIndex {
    override val facadeFileMap: MutableMap<FqName, MutableSet<KotlinStandaloneIndexEntry<KtFile>>> = mutableMapOf()
    override val multiFileClassPartMap: MutableMap<FqName, MutableSet<KotlinStandaloneIndexEntry<KtFile>>> = mutableMapOf()
    override val scriptMap: MutableMap<FqName, MutableSet<KotlinStandaloneIndexEntry<KtScript>>> = mutableMapOf()

    override val classesByClassId: MutableMap<ClassId, MutableSet<KotlinStandaloneIndexEntry<KtClassOrObject>>> = mutableMapOf()
    override val typeAliasesByClassId: MutableMap<ClassId, MutableSet<KotlinStandaloneIndexEntry<KtTypeAlias>>> = mutableMapOf()
    override val topLevelFunctionsByCallableId: MutableMap<CallableId, MutableSet<KotlinStandaloneIndexEntry<KtNamedFunction>>> =
        mutableMapOf()
    override val topLevelPropertiesByCallableId: MutableMap<CallableId, MutableSet<KotlinStandaloneIndexEntry<KtProperty>>> =
        mutableMapOf()

    override val classLikeDeclarationsByPackage: MutableMap<FqName, MutableSet<KotlinStandaloneIndexEntry<KtClassLikeDeclaration>>> =
        mutableMapOf()
    override val topLevelCallablesByPackage: MutableMap<FqName, MutableSet<KotlinStandaloneIndexEntry<KtCallableDeclaration>>> =
        mutableMapOf()

    override val classesBySupertypeName: MutableMap<Name, MutableSet<KotlinStandaloneIndexEntry<KtClassOrObject>>> = mutableMapOf()
    override val inheritableTypeAliasesByAliasedName: MutableMap<Name, MutableSet<KotlinStandaloneIndexEntry<KtTypeAlias>>> =
        mutableMapOf()

    fun indexFile(file: KotlinStandaloneIndexedFile, records: List<KotlinStandaloneIndexRecord>) {
        for (record in records) {
            when (record) {
                is KotlinStandaloneIndexRecord.FacadeFile ->
                    facadeFileMap.addEntry(record.packageFqName, KotlinStandaloneIndexEntry(file, record.ordinal, name = null))

                is KotlinStandaloneIndexRecord.MultifileClassPart ->
                    multiFileClassPartMap.addEntry(record.facadeFqName, KotlinStandaloneIndexEntry(file, record.ordinal, name = null))

                is KotlinStandaloneIndexRecord.Script ->
                    scriptMap.addEntry(record.fqName, KotlinStandaloneIndexEntry(file, record.ordinal, name = null))

                is KotlinStandaloneIndexRecord.ClassOrObject -> {
                    val entry = KotlinStandaloneIndexEntry<KtClassOrObject>(file, record.ordinal, record.name)
                    indexClassLikeDeclaration(record.classId, entry, classesByClassId)

                    for (superName in record.superNames) {
                        classesBySupertypeName.addEntry(superName, entry)
                    }
                }

                is KotlinStandaloneIndexRecord.TypeAlias -> {
                    val entry = KotlinStandaloneIndexEntry<KtTypeAlias>(file, record.ordinal, record.name)
                    indexClassLikeDeclaration(record.classId, entry, typeAliasesByClassId)

                    for (expandedName in record.inheritableSimpleNames) {
                        inheritableTypeAliasesByAliasedName.addEntry(expandedName, entry)
                    }
                }

                is KotlinStandaloneIndexRecord.TopLevelFunction -> {
                    val entry = KotlinStandaloneIndexEntry<KtNamedFunction>(file, record.ordinal, record.callableId.callableName)
                    indexTopLevelCallable(record.callableId, entry, topLevelFunctionsByCallableId)
                }

                is KotlinStandaloneIndexRecord.TopLevelProperty -> {
                    val entry = KotlinStandaloneIndexEntry<KtProperty>(file, record.ordinal, record.callableId.callableName)
                    indexTopLevelCallable(record.callableId, entry, topLevelPropertiesByCallableId)
                }
            }
        }
    }

    private fun <T : KtClassLikeDeclaration> indexClassLikeDeclaration(
        classId: ClassId?,
        entry: KotlinStandaloneIndexEntry<T>,
        classLikeDeclarationsByClassId: MutableMap<ClassId, MutableSet<KotlinStandaloneIndexEntry<T>>>,
    ) {
        if (classId == null) return

        classLikeDeclarationsByClassId.addEntry(classId, entry)
        classLikeDeclarationsByPackage.addEntry(classId.packageFqName, entry)
    }

    private fun <T : KtCallableDeclaration> indexTopLevelCallable(
        callableId: CallableId,
        entry: KotlinStandaloneIndexEntry<T>,
        callablesByCallableId: MutableMap<CallableId, MutableSet<KotlinStandaloneIndexEntry<T>>>,
    ) {
        callablesByCallableId.addEntry(callableId, entry)
        topLevelCallablesByPackage.addEntry(callableId.packageName, entry)
    }

    private fun <K, T : PsiElement> MutableMap<K, MutableSet<KotlinStandaloneIndexEntry<T>>>.addEntry(
        key: K,
        entry: KotlinStandaloneIndexEntry<T>,
    ) {
        computeIfAbsent(key) { mutableSetOf() }.add(entry)
    }
}
