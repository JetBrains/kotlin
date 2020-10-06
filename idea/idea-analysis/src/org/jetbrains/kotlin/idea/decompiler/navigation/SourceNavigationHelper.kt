/*
 * Copyright 2000-2017 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.decompiler.navigation

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.OrderEntry
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.util.containers.ContainerUtil
import gnu.trove.THashSet
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.fileClasses.JvmMultifileClassPartInfo
import org.jetbrains.kotlin.fileClasses.fileClassInfo
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.idea.caches.project.BinaryModuleInfo
import org.jetbrains.kotlin.idea.caches.project.ScriptDependenciesInfo
import org.jetbrains.kotlin.idea.caches.project.getBinaryLibrariesModuleInfos
import org.jetbrains.kotlin.idea.caches.project.getLibrarySourcesModuleInfos
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.decompiler.navigation.MemberMatching.*
import org.jetbrains.kotlin.idea.stubindex.*
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.util.isExpectDeclaration
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.debugText.getDebugText
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

object SourceNavigationHelper {
    private val LOG = Logger.getInstance(SourceNavigationHelper::class.java)

    enum class NavigationKind {
        CLASS_FILES_TO_SOURCES,
        SOURCES_TO_CLASS_FILES
    }

    private var forceResolve = false

    @TestOnly
    fun setForceResolve(forceResolve: Boolean) {
        SourceNavigationHelper.forceResolve = forceResolve
    }

    private fun targetScopes(declaration: KtNamedDeclaration, navigationKind: NavigationKind): List<GlobalSearchScope> {
        val containingFile = declaration.containingKtFile
        val vFile = containingFile.virtualFile ?: return emptyList()

        return when (navigationKind) {
            NavigationKind.CLASS_FILES_TO_SOURCES -> {
                val binaryModuleInfos = getBinaryLibrariesModuleInfos(declaration.project, vFile)
                val primaryScope = binaryModuleInfos.mapNotNull { it.sourcesModuleInfo?.sourceScope() }.union()
                val additionalScope = binaryModuleInfos.flatMap {
                    it.associatedCommonLibraries()
                }.mapNotNull { it.sourcesModuleInfo?.sourceScope() }.union()

                if (binaryModuleInfos.any { it is ScriptDependenciesInfo }) {
                    // NOTE: this is a workaround for https://github.com/gradle/gradle/issues/13783:
                    // script configuration for *.gradle.kts files doesn't include sources for included plugins
                    primaryScope + additionalScope + ProjectScope.getContentScope(containingFile.project)
                } else {
                    primaryScope + additionalScope
                }
            }

            NavigationKind.SOURCES_TO_CLASS_FILES -> {
                if (containingFile.fileClassInfo is JvmMultifileClassPartInfo) {
                    // if the asked element is multifile classs, it might be compiled into .kotlin_metadata and .class
                    // but we don't have support of metadata declarations in light classes and in reference search (without
                    // acceptOverrides). That's why we include only .class jar in the scope.
                    val psiClass = JavaElementFinder.getInstance(containingFile.project)
                        .findClass(containingFile.javaFileFacadeFqName.asString(), declaration.resolveScope)
                    if (psiClass != null) {
                        return getBinaryLibrariesModuleInfos(declaration.project, psiClass.containingFile.virtualFile)
                            .map { it.binariesScope() }.union()
                    }
                }
                getLibrarySourcesModuleInfos(
                    declaration.project,
                    vFile
                ).map { it.binariesModuleInfo.binariesScope() }.union()
            }
        }
    }

    private fun BinaryModuleInfo.associatedCommonLibraries(): List<BinaryModuleInfo> {
        val platform = platform
        if (platform == null || platform.isCommon()) return emptyList()

        return dependencies().filterIsInstance<BinaryModuleInfo>().filter {
            it.platform.isCommon()
        }
    }

    private fun Collection<GlobalSearchScope>.union(): List<GlobalSearchScope> =
        if (this.isNotEmpty()) listOf(GlobalSearchScope.union(this.toTypedArray())) else emptyList()

    private fun haveRenamesInImports(files: Collection<KtFile>) = files.any { file -> file.importDirectives.any { it.aliasName != null } }

    private fun findSpecialProperty(memberName: Name, containingClass: KtClass): KtNamedDeclaration? {
        // property constructor parameters
        val constructorParameters = containingClass.primaryConstructorParameters
        for (constructorParameter in constructorParameters) {
            if (memberName == constructorParameter.nameAsName && constructorParameter.hasValOrVar()) {
                return constructorParameter
            }
        }

        // enum entries
        if (containingClass.hasModifier(KtTokens.ENUM_KEYWORD)) {
            for (enumEntry in ContainerUtil.findAll<KtDeclaration, KtEnumEntry>(containingClass.declarations, KtEnumEntry::class.java)) {
                if (memberName == enumEntry.nameAsName) {
                    return enumEntry
                }
            }
        }
        return null
    }

    private fun convertPropertyOrFunction(
        declaration: KtNamedDeclaration,
        navigationKind: NavigationKind
    ): KtNamedDeclaration? {
        if (declaration is KtPrimaryConstructor) {
            val sourceClassOrObject = findClassOrObject(declaration.getContainingClassOrObject(), navigationKind)
            return sourceClassOrObject?.primaryConstructor ?: sourceClassOrObject
        }

        val memberNameAsString = declaration.name
        if (memberNameAsString == null) {
            LOG.debug("Declaration with null name:" + declaration.getDebugText())
            return null
        }
        val memberName = Name.identifier(memberNameAsString)

        val decompiledContainer = declaration.parent

        var candidates: Collection<KtNamedDeclaration>
        when (decompiledContainer) {
            is KtFile -> candidates = getInitialTopLevelCandidates(declaration, navigationKind)
            is KtClassBody -> {
                val decompiledClassOrObject = decompiledContainer.getParent() as KtClassOrObject
                val sourceClassOrObject = findClassOrObject(decompiledClassOrObject, navigationKind)

                candidates = sourceClassOrObject?.let {
                    getInitialMemberCandidates(sourceClassOrObject, memberName, declaration::class.java)
                }.orEmpty()

                if (candidates.isEmpty()) {
                    if (declaration is KtProperty && sourceClassOrObject is KtClass) {
                        return findSpecialProperty(memberName, sourceClassOrObject)
                    }
                }
            }
            else -> throw IllegalStateException(
                "Unexpected container of " +
                        (if (navigationKind == NavigationKind.CLASS_FILES_TO_SOURCES) "decompiled" else "source") +
                        " declaration: " +
                        decompiledContainer::class.java.simpleName
            )
        }

        if (candidates.isEmpty()) {
            return null
        }

        if (!forceResolve) {
            candidates = candidates.filter { sameReceiverPresenceAndParametersCount(it, declaration) }

            if (candidates.size <= 1) {
                return candidates.firstOrNull()
            }

            if (!haveRenamesInImports(candidates.getContainingFiles())) {
                candidates = candidates.filter { receiverAndParametersShortTypesMatch(it, declaration) }


                if (candidates.size <= 1) {
                    return candidates.firstOrNull()
                }
            }
        }

        for (candidate in candidates) {
            val candidateDescriptor = candidate.resolveToDescriptorIfAny() as? CallableDescriptor ?: continue
            if (receiversMatch(declaration, candidateDescriptor)
                && valueParametersTypesMatch(declaration, candidateDescriptor)
                && typeParametersMatch(declaration as KtTypeParameterListOwner, candidateDescriptor.typeParameters)
            ) {
                return candidate
            }
        }

        return null
    }

    private fun <T : KtNamedDeclaration> findFirstMatchingInIndex(
        entity: T,
        navigationKind: NavigationKind,
        index: StringStubIndexExtension<T>
    ): T? {
        val classFqName = entity.fqName ?: return null
        return targetScopes(entity, navigationKind).firstNotNullResult { scope ->
            index.get(classFqName.asString(), entity.project, scope).minByOrNull { it.isExpectDeclaration() }
        }
    }

    private fun findClassOrObject(decompiledClassOrObject: KtClassOrObject, navigationKind: NavigationKind): KtClassOrObject? {
        return findFirstMatchingInIndex<KtClassOrObject>(decompiledClassOrObject, navigationKind, KotlinFullClassNameIndex.getInstance())
    }

    private fun getInitialTopLevelCandidates(
        declaration: KtNamedDeclaration,
        navigationKind: NavigationKind
    ): Collection<KtNamedDeclaration> {
        val scopes = targetScopes(declaration, navigationKind)

        val index: StringStubIndexExtension<out KtNamedDeclaration> = when (declaration) {
            is KtNamedFunction -> KotlinTopLevelFunctionFqnNameIndex.getInstance()
            is KtProperty -> KotlinTopLevelPropertyFqnNameIndex.getInstance()
            else -> throw IllegalArgumentException("Neither function nor declaration: " + declaration::class.java.name)
        }

        return scopes.flatMap { scope ->
            index.get(declaration.fqName!!.asString(), declaration.project, scope).sortedBy { it.isExpectDeclaration() }
        }
    }

    private fun getInitialMemberCandidates(
        sourceClassOrObject: KtClassOrObject,
        name: Name,
        declarationClass: Class<out KtNamedDeclaration>
    ) = sourceClassOrObject.declarations.filterIsInstance(declarationClass).filter { declaration ->
        name == declaration.nameAsSafeName
    }

    fun getOriginalClass(classOrObject: KtClassOrObject): PsiClass? {
        // Copied from JavaPsiImplementationHelperImpl:getOriginalClass()
        val fqName = classOrObject.fqName ?: return null

        val file = classOrObject.containingKtFile

        val vFile = file.virtualFile
        val project = file.project

        val idx = ProjectRootManager.getInstance(project).fileIndex

        if (vFile == null || !idx.isInLibrarySource(vFile)) return null
        val orderEntries = THashSet<OrderEntry>(idx.getOrderEntriesForFile(vFile))

        return JavaPsiFacade.getInstance(project).findClass(fqName.asString(), object : GlobalSearchScope(project) {
            override fun compare(file1: VirtualFile, file2: VirtualFile): Int {
                return 0
            }

            override fun contains(file: VirtualFile): Boolean {
                if (file == vFile) return false
                val entries = idx.getOrderEntriesForFile(file)
                return entries.any { orderEntries.contains(it) }
            }

            override fun isSearchInModuleContent(aModule: Module): Boolean {
                return false
            }

            override fun isSearchInLibraries(): Boolean {
                return true
            }
        })
    }

    fun getNavigationElement(declaration: KtDeclaration) = navigateToDeclaration(declaration, NavigationKind.CLASS_FILES_TO_SOURCES)

    fun getOriginalElement(declaration: KtDeclaration) = navigateToDeclaration(declaration, NavigationKind.SOURCES_TO_CLASS_FILES)

    private fun navigateToDeclaration(
        from: KtDeclaration,
        navigationKind: NavigationKind
    ): KtDeclaration {
        if (DumbService.isDumb(from.project)) return from

        when (navigationKind) {
            NavigationKind.CLASS_FILES_TO_SOURCES -> if (!from.containingKtFile.isCompiled) return from
            NavigationKind.SOURCES_TO_CLASS_FILES -> {
                val file = from.containingFile
                if (file is KtFile && file.isCompiled) return from
                if (!ProjectRootsUtil.isInContent(
                        from,
                        includeProjectSource = false,
                        includeLibrarySource = true,
                        includeLibraryClasses = false,
                        includeScriptDependencies = true,
                        includeScriptsOutsideSourceRoots = false
                    )
                ) return from
                if (KtPsiUtil.isLocal(from)) return from
            }
        }

        return from.accept(SourceAndDecompiledConversionVisitor(navigationKind), Unit) ?: from
    }

    private class SourceAndDecompiledConversionVisitor(private val navigationKind: NavigationKind) : KtVisitor<KtDeclaration?, Unit>() {

        override fun visitNamedFunction(function: KtNamedFunction, data: Unit) = convertPropertyOrFunction(function, navigationKind)

        override fun visitProperty(property: KtProperty, data: Unit) = convertPropertyOrFunction(property, navigationKind)

        override fun visitObjectDeclaration(declaration: KtObjectDeclaration, data: Unit) = findClassOrObject(declaration, navigationKind)

        override fun visitClass(klass: KtClass, data: Unit) = findClassOrObject(klass, navigationKind)

        override fun visitTypeAlias(typeAlias: KtTypeAlias, data: Unit) =
            findFirstMatchingInIndex(typeAlias, navigationKind, KotlinTopLevelTypeAliasFqNameIndex.getInstance())

        override fun visitParameter(parameter: KtParameter, data: Unit): KtDeclaration? {
            val callableDeclaration = parameter.parent.parent as KtCallableDeclaration
            val parameters = callableDeclaration.valueParameters
            val index = parameters.indexOf(parameter)

            val sourceCallable = callableDeclaration.accept(this, Unit) as? KtCallableDeclaration ?: return null
            val sourceParameters = sourceCallable.valueParameters
            if (sourceParameters.size != parameters.size) return null
            return sourceParameters[index]
        }

        override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor, data: Unit) =
            convertPropertyOrFunction(constructor, navigationKind)

        override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor, data: Unit) =
            convertPropertyOrFunction(constructor, navigationKind)
    }
}

private fun Collection<KtNamedDeclaration>.getContainingFiles(): Collection<KtFile> = mapNotNullTo(LinkedHashSet()) {
    it.containingFile as? KtFile
}
