/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.resolve.extensions

import com.intellij.mock.MockProject
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtension
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtensionFile
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtensionNavigationTargetsProvider
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtensionProvider
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.test.framework.services.environmentManager
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.test.services.TestServices

class KaResolveExtensionProviderForTest(
    private val files: List<KaResolveExtensionFile>,
    private val packages: Set<FqName>,
    private val shadowedScope: GlobalSearchScope,
    private val hasResolveExtension: (KtModule) -> Boolean = { true }
) : KaResolveExtensionProvider() {
    override fun provideExtensionsFor(module: KtModule): List<KaResolveExtension> {
        if (!hasResolveExtension(module)) return emptyList()
        return listOf(KaResolveExtensionForTest(files, packages, shadowedScope))
    }

    fun register(testServices: TestServices) {
        val project = testServices.environmentManager.getProject() as MockProject
        val extensionPoint = project.extensionArea.getExtensionPoint(EP_NAME)
        extensionPoint.registerExtension(this, project)
    }
}

class KaResolveExtensionForTest(
    private val files: List<KaResolveExtensionFile>,
    private val packages: Set<FqName>,
    private val shadowedScope: GlobalSearchScope,
) : KaResolveExtension() {
    override fun getKtFiles(): List<KaResolveExtensionFile> = files
    override fun getContainedPackages(): Set<FqName> = packages
    override fun getShadowedScope(): GlobalSearchScope = shadowedScope
}

class KaResolveExtensionFileForTests(
    private val fileName: String,
    private val packageName: FqName,
    topLevelClassifiersNames: Set<String>,
    topLevelCallableNames: Set<String>,
    private val fileText: String,
    private val navigationTargetsProvider: KaResolveExtensionNavigationTargetsProvider? = null
) : KaResolveExtensionFile() {

    private val topLevelClassifiersNames: Set<Name> = topLevelClassifiersNames.mapTo(mutableSetOf()) { Name.identifier(it) }
    private val topLevelCallableNames: Set<Name> = topLevelCallableNames.mapTo(mutableSetOf()) { Name.identifier(it) }
    override fun getFileName(): String = fileName
    override fun getFilePackageName(): FqName = packageName
    override fun getTopLevelClassifierNames(): Set<Name> = topLevelClassifiersNames
    override fun getTopLevelCallableNames(): Set<Name> = topLevelCallableNames

    override fun buildFileText(): String = fileText

    private object ResolveExtensionNavigationTargetProviderForTest : KaResolveExtensionNavigationTargetsProvider() {
        override fun KaSession.getNavigationTargets(element: KtElement): Collection<PsiElement> =
            listOf(KtResolveExtensionNavigationTargetPsiElementForTest(element))
    }

    override fun createNavigationTargetsProvider(): KaResolveExtensionNavigationTargetsProvider =
        navigationTargetsProvider ?: ResolveExtensionNavigationTargetProviderForTest
}

fun KtElement.getDescription(): String = buildString {
    val declaration = when (this@getDescription) {
        is KtDeclaration -> this@getDescription
        else -> {
            append("${this@getDescription.javaClass.simpleName} in ")
            getStrictParentOfType<KtDeclaration>() ?: containingKtFile
        }
    }
    append("${declaration.javaClass.simpleName} ${declaration.name}")
}

class KtResolveExtensionNavigationTargetPsiElementForTest(val originalElement: KtElement) : PsiElement by originalElement {
    override fun toString() = "[Resolve extension navigation target for test for ${originalElement.getDescription()}]"
}

