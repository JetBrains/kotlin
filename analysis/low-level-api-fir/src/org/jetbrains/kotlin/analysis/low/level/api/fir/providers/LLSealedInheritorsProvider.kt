/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.project.structure.KotlinModuleDependentsProvider
import org.jetbrains.kotlin.analysis.project.structure.KtDanglingFileModule
import org.jetbrains.kotlin.analysis.providers.KotlinDirectInheritorsProvider
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProvider
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProviderInternals
import org.jetbrains.kotlin.fir.declarations.sealedInheritorsAttr
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClass
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.filter
import kotlin.collections.map
import kotlin.collections.mapNotNull
import kotlin.collections.plus
import kotlin.collections.sortedBy
import kotlin.let

/**
 * [LLSealedInheritorsProvider] is the LL FIR implementation of [SealedClassInheritorsProvider] for both the IDE and Standalone mode.
 */
@OptIn(SealedClassInheritorsProviderInternals::class)
internal class LLSealedInheritorsProvider(private val project: Project) : SealedClassInheritorsProvider() {
    val cache = ConcurrentHashMap<ClassId, List<ClassId>>()

    override fun getSealedClassInheritors(firClass: FirRegularClass): List<ClassId> {
        // Classes from binary libraries which are deserialized from class files (but not stubs) will have their `sealedInheritorsAttr` set
        // from metadata.
        firClass.sealedInheritorsAttr?.let { return it.value }

        val classId = firClass.classId

        // Local classes cannot be sealed.
        if (classId.isLocal) {
            return emptyList()
        }

        return cache.computeIfAbsent(classId) { searchInheritors(firClass) }
    }

    /**
     * Some notes about the search:
     *
     *  - A Java class cannot legally extend a sealed Kotlin class (even in the same package), so we don't need to search for Java class
     *    inheritors.
     *  - Technically, we could use a package scope to narrow the search, but the search is already sufficiently narrow because it uses
     *    supertype indices and is confined to the current `KtModule` in most cases (except for 'expect' classes). Finding a `PsiPackage`
     *    for a `PackageScope` is not cheap, hence the decision to avoid it. If a `PackageScope` is needed in the future, it'd be best to
     *    extract a `PackageNameScope` which operates just with the qualified package name, to avoid `PsiPackage`. (At the time of writing,
     *    this is possible with the implementation of `PackageScope`.)
     *  - We ignore local classes to avoid lazy resolve contract violations.
     *    See KT-63795.
     *  - For `expect` declarations, the search scope includes all modules with a dependsOn dependency on the containing module.
     *    At the same time, `actual` declarations are restricted to the same module and require no special handling.
     *    See KT-45842.
     *  - KMP libraries are not yet supported.
     *    See KT-65591.
     */
    private fun searchInheritors(firClass: FirClass): List<ClassId> {
        val ktClass = firClass.psi as? KtClass ?: return emptyList()

        val ktModule = when (val classKtModule = firClass.llFirModuleData.ktModule) {
            is KtDanglingFileModule -> classKtModule.contextModule
            else -> classKtModule
        }

        // `FirClass.isExpect` does not depend on the `STATUS` phase because it's already set during FIR building.
        val scope = if (firClass.isExpect) {
            val refinementDependents = KotlinModuleDependentsProvider.getInstance(project).getRefinementDependents(ktModule)
            GlobalSearchScope.union(refinementDependents.map { it.contentScope } + ktModule.contentScope)
        } else {
            ktModule.contentScope
        }

        // Currently, it is possible to have kotlin-only sealed hierarchy
        val kotlinScope = GlobalSearchScope.getScopeRestrictedByFileTypes(
            scope,
            KotlinFileType.INSTANCE,
            JavaClassFileType.INSTANCE,
        )

        return searchInScope(ktClass, firClass.classId, kotlinScope)
    }

    private fun searchInScope(ktClass: KtClass, classId: ClassId, scope: GlobalSearchScope): List<ClassId> =
        KotlinDirectInheritorsProvider.getInstance(project)
            .getDirectKotlinInheritors(ktClass, scope, includeLocalInheritors = false)
            .mapNotNull { it.getClassId() }
            .filter { it.packageFqName == classId.packageFqName }
            // Enforce a deterministic order on the result, e.g. for stable test output.
            .sortedBy { it.toString() }
            .ifEmpty { emptyList() }
}
