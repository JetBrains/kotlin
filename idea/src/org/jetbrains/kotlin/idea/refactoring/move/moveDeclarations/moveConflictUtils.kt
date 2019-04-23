/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.module.impl.scopes.JdkScope
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.JdkOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.refactoring.util.MoveRenameUsageInfo
import com.intellij.refactoring.util.NonCodeUsageInfo
import com.intellij.refactoring.util.RefactoringUIUtil
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewTypeLocation
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.MutablePackageFragmentDescriptor
import org.jetbrains.kotlin.idea.caches.project.forcedModuleInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfoByVirtualFile
import org.jetbrains.kotlin.idea.caches.project.implementedModules
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaMemberDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.idea.core.isInTestSourceContentKotlinAware
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.idea.project.forcedTargetPlatform
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.refactoring.getUsageContext
import org.jetbrains.kotlin.idea.refactoring.move.KotlinMoveUsage
import org.jetbrains.kotlin.idea.search.and
import org.jetbrains.kotlin.idea.search.not
import org.jetbrains.kotlin.idea.util.projectStructure.getModule
import org.jetbrains.kotlin.idea.util.projectStructure.module
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.lazy.descriptors.findPackageFragmentForFile
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny
import org.jetbrains.kotlin.util.isJavaDescriptor
import org.jetbrains.kotlin.util.supertypesWithAny
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.util.*

class MoveConflictChecker(
    private val project: Project,
    private val elementsToMove: Collection<KtElement>,
    private val moveTarget: KotlinMoveTarget,
    contextElement: KtElement,
    private val doNotGoIn: Collection<KtElement> = emptyList(),
    allElementsToMove: Collection<PsiElement>? = null
) {
    private val resolutionFacade = contextElement.getResolutionFacade()

    private val fakeFile = KtPsiFactory(project).createFile("")

    private val allElementsToMove = allElementsToMove ?: elementsToMove

    private fun PackageFragmentDescriptor.withSource(sourceFile: KtFile): PackageFragmentDescriptor {
        return object : PackageFragmentDescriptor by this {
            override fun getOriginal() = this
            override fun getSource() = KotlinSourceElement(sourceFile)
        }
    }

    private fun getModuleDescriptor(sourceFile: VirtualFile) =
        getModuleInfoByVirtualFile(
            project,
            sourceFile
        )?.let { resolutionFacade.findModuleDescriptor(it) }

    private fun KotlinMoveTarget.getContainerDescriptor(): DeclarationDescriptor? {
        return when (this) {
            is KotlinMoveTargetForExistingElement -> {
                val targetElement = targetElement
                when (targetElement) {
                    is KtNamedDeclaration -> resolutionFacade.resolveToDescriptor(targetElement as KtNamedDeclaration)

                    is KtFile -> {
                        val packageFragment =
                            targetElement
                                .findModuleDescriptor()
                                .findPackageFragmentForFile(targetElement as KtFile)

                        packageFragment?.withSource(targetElement as KtFile)
                    }

                    else -> null
                }
            }

            is KotlinDirectoryBasedMoveTarget -> {
                val packageFqName = targetContainerFqName ?: return null
                val targetModuleDescriptor = targetScope?.let { getModuleDescriptor(it) ?: return null }
                    ?: resolutionFacade.moduleDescriptor
                MutablePackageFragmentDescriptor(targetModuleDescriptor, packageFqName).withSource(fakeFile)
            }

            else -> null
        }
    }

    private fun DeclarationDescriptor.isVisibleIn(where: DeclarationDescriptor): Boolean {
        return when {
            this !is DeclarationDescriptorWithVisibility -> true
            !Visibilities.isVisibleIgnoringReceiver(this, where) -> false
            this is ConstructorDescriptor -> Visibilities.isVisibleIgnoringReceiver(containingDeclaration, where)
            else -> true
        }
    }

    private fun DeclarationDescriptor.wrap(
        newContainer: DeclarationDescriptor? = null,
        newVisibility: Visibility? = null
    ): DeclarationDescriptor? {
        if (newContainer == null && newVisibility == null) return this

        val wrappedDescriptor = this
        return when (wrappedDescriptor) {
            // We rely on visibility not depending on more specific type of CallableMemberDescriptor
            is CallableMemberDescriptor -> object : CallableMemberDescriptor by wrappedDescriptor as CallableMemberDescriptor {
                override fun getOriginal() = this
                override fun getContainingDeclaration() = newContainer ?: (wrappedDescriptor as CallableMemberDescriptor).containingDeclaration
                override fun getVisibility(): Visibility = newVisibility ?: (wrappedDescriptor as CallableMemberDescriptor).visibility
                override fun getSource() =
                    newContainer?.let { SourceElement { DescriptorUtils.getContainingSourceFile(it) } } ?: (wrappedDescriptor as CallableMemberDescriptor).source
            }
            is ClassDescriptor -> object : ClassDescriptor by wrappedDescriptor as ClassDescriptor {
                override fun getOriginal() = this
                override fun getContainingDeclaration() = newContainer ?: (wrappedDescriptor as ClassDescriptor).containingDeclaration
                override fun getVisibility(): Visibility = newVisibility ?: (wrappedDescriptor as ClassDescriptor).visibility
                override fun getSource() =
                    newContainer?.let { SourceElement { DescriptorUtils.getContainingSourceFile(it) } } ?: (wrappedDescriptor as ClassDescriptor).source
            }
            else -> null
        }
    }

    private fun DeclarationDescriptor.asPredicted(
        newContainer: DeclarationDescriptor,
        actualVisibility: Visibility?
    ): DeclarationDescriptor? {
        val visibility = actualVisibility ?: (this as? DeclarationDescriptorWithVisibility)?.visibility ?: return null
        val adjustedVisibility = if (visibility == Visibilities.PROTECTED && newContainer is PackageFragmentDescriptor) {
            Visibilities.PUBLIC
        } else {
            visibility
        }
        return wrap(newContainer, adjustedVisibility)
    }

    private fun DeclarationDescriptor.visibilityAsViewedFromJava(): Visibility? {
        if (this !is DeclarationDescriptorWithVisibility) return null
        return when (visibility) {
            Visibilities.PRIVATE -> {
                if (this is ClassDescriptor && DescriptorUtils.isTopLevelDeclaration(this)) JavaVisibilities.PACKAGE_VISIBILITY else null
            }
            Visibilities.PROTECTED -> JavaVisibilities.PROTECTED_AND_PACKAGE
            else -> null
        }
    }

    private fun render(declaration: PsiElement) = RefactoringUIUtil.getDescription(declaration, false)

    // Based on RefactoringConflictsUtil.analyzeModuleConflicts
    fun analyzeModuleConflictsInUsages(
        project: Project,
        usages: Collection<UsageInfo>,
        targetScope: VirtualFile,
        conflicts: MultiMap<PsiElement, String>
    ) {
        val targetModule = targetScope.getModule(project) ?: return

        val isInTestSources = ModuleRootManager.getInstance(targetModule).fileIndex.isInTestSourceContentKotlinAware(targetScope)
        NextUsage@ for (usage in usages) {
            val element = usage.element ?: continue
            if (PsiTreeUtil.getParentOfType(element, PsiImportStatement::class.java, false) != null) continue
            if (isToBeMoved(element)) continue@NextUsage

            val resolveScope = element.resolveScope
            if (resolveScope.isSearchInModuleContent(targetModule, isInTestSources)) continue

            val usageModule = element.module ?: continue
            val scopeDescription = RefactoringUIUtil.getDescription(element.getUsageContext(), true)
            val referencedElement = (if (usage is MoveRenameUsageInfo) (usage as MoveRenameUsageInfo).referencedElement else usage.element) ?: error(usage)
            val message = if (usageModule == targetModule && isInTestSources) {
                RefactoringBundle.message(
                    "0.referenced.in.1.will.not.be.accessible.from.production.of.module.2",
                    RefactoringUIUtil.getDescription(referencedElement, true),
                    scopeDescription,
                    CommonRefactoringUtil.htmlEmphasize(usageModule.name)
                )
            } else {
                RefactoringBundle.message(
                    "0.referenced.in.1.will.not.be.accessible.from.module.2",
                    RefactoringUIUtil.getDescription(referencedElement, true),
                    scopeDescription,
                    CommonRefactoringUtil.htmlEmphasize(usageModule.name)
                )
            }
            conflicts.putValue(referencedElement, CommonRefactoringUtil.capitalize(message))
        }
    }

    fun checkModuleConflictsInUsages(externalUsages: MutableSet<UsageInfo>, conflicts: MultiMap<PsiElement, String>) {
        val newConflicts = MultiMap<PsiElement, String>()
        val targetScope = moveTarget.targetScope ?: return

        analyzeModuleConflictsInUsages(project, externalUsages, targetScope, newConflicts)
        if (!newConflicts.isEmpty) {
            val referencedElementsToSkip = newConflicts.keySet().mapNotNullTo(HashSet()) { it.namedUnwrappedElement }
            externalUsages.removeIf {
                it is MoveRenameUsageInfo &&
                        (it as MoveRenameUsageInfo).referencedElement?.namedUnwrappedElement?.let { it in referencedElementsToSkip } ?: false
            }
            conflicts.putAllValues(newConflicts)
        }
    }

    companion object {
        private val DESCRIPTOR_RENDERER_FOR_COMPARISON = DescriptorRenderer.withOptions {
            withDefinedIn = true
            classifierNamePolicy = ClassifierNamePolicy.FULLY_QUALIFIED
            modifiers = emptySet()
            withoutTypeParameters = true
            parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE
            includeAdditionalModifiers = false
            renderUnabbreviatedType = false
            withoutSuperTypes = true
        }
    }

    private fun Module.getScopeWithPlatformAwareDependencies(): SearchScope {
        val baseScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(this)

        val targetPlatform = TargetPlatformDetector.getPlatform(this)
        if (targetPlatform is JvmPlatform) return baseScope

        return ModuleRootManager.getInstance(this)
            .orderEntries
            .filterIsInstance<JdkOrderEntry>()
            .fold(baseScope as SearchScope) { scope, jdkEntry -> scope and !JdkScope(project, jdkEntry) }
    }

    fun checkModuleConflictsInDeclarations(
        internalUsages: MutableSet<UsageInfo>,
        conflicts: MultiMap<PsiElement, String>
    ) {
        val targetScope = moveTarget.targetScope ?: return
        val targetModule = targetScope.getModule(project) ?: return
        val resolveScope = targetModule.getScopeWithPlatformAwareDependencies()

        fun isInScope(targetElement: PsiElement, targetDescriptor: DeclarationDescriptor): Boolean {
            if (targetElement in resolveScope) return true
            if (targetElement.manager.isInProject(targetElement)) return false

            val fqName = targetDescriptor.importableFqName ?: return true
            val importableDescriptor = targetDescriptor.getImportableDescriptor()

            val targetModuleInfo = getModuleInfoByVirtualFile(project, targetScope)
            val dummyFile = KtPsiFactory(targetElement.project).createFile("dummy.kt", "").apply {
                forcedModuleInfo = targetModuleInfo
                forcedTargetPlatform = TargetPlatformDetector.getPlatform(targetModule)
            }

            val newTargetDescriptors = dummyFile.resolveImportReference(fqName)

            if (importableDescriptor is TypeAliasDescriptor
                && newTargetDescriptors.any {
                    it is ClassDescriptor && (it as ClassDescriptor).isExpect && it.importableFqName == importableDescriptor.importableFqName
                }
            ) return true

            val renderedImportableTarget = DESCRIPTOR_RENDERER_FOR_COMPARISON.render(importableDescriptor)
            val renderedTarget by lazy { DESCRIPTOR_RENDERER_FOR_COMPARISON.render(targetDescriptor) }

            return newTargetDescriptors.any {
                if (DESCRIPTOR_RENDERER_FOR_COMPARISON.render(it) != renderedImportableTarget) return@any false
                if (importableDescriptor == targetDescriptor) return@any true

                val candidateDescriptors: Collection<DeclarationDescriptor> = when (targetDescriptor) {
                    is ConstructorDescriptor -> {
                        (it as? ClassDescriptor)?.constructors ?: emptyList<DeclarationDescriptor>()
                    }

                    is PropertyAccessorDescriptor -> {
                        (it as? PropertyDescriptor)
                            ?.let { if (targetDescriptor is PropertyGetterDescriptor) it.getter else it.setter }
                            ?.let { listOf(it) }
                            ?: emptyList<DeclarationDescriptor>()
                    }

                    else -> emptyList()
                }

                candidateDescriptors.any { DESCRIPTOR_RENDERER_FOR_COMPARISON.render(it) == renderedTarget }
            }
        }

        val referencesToSkip = HashSet<KtReferenceExpression>()
        for (declaration in elementsToMove - doNotGoIn) {
            if (declaration.module == targetModule) continue

            declaration.forEachDescendantOfType<KtReferenceExpression> { refExpr ->
                // NB: for unknown reason, refExpr.resolveToCall() does not work here
                val targetDescriptor =
                    refExpr.analyze(BodyResolveMode.PARTIAL)[BindingContext.REFERENCE_TARGET, refExpr] ?: return@forEachDescendantOfType

                if (KotlinBuiltIns.isBuiltIn(targetDescriptor)) return@forEachDescendantOfType

                val target = DescriptorToSourceUtilsIde.getAnyDeclaration(project, targetDescriptor) ?: return@forEachDescendantOfType

                if (isToBeMoved(target)) return@forEachDescendantOfType

                if (isInScope(target, targetDescriptor)) return@forEachDescendantOfType
                if (target is KtTypeParameter) return@forEachDescendantOfType

                val superMethods = SmartSet.create<PsiMethod>()
                target.toLightMethods().forEach { superMethods += it.findDeepestSuperMethods() }
                if (superMethods.any { isInScope(it, targetDescriptor) }) return@forEachDescendantOfType

                val refContainer = refExpr.getStrictParentOfType<KtNamedDeclaration>() ?: return@forEachDescendantOfType
                val scopeDescription = RefactoringUIUtil.getDescription(refContainer, true)
                val message = RefactoringBundle.message(
                    "0.referenced.in.1.will.not.be.accessible.in.module.2",
                    RefactoringUIUtil.getDescription(target, true),
                    scopeDescription,
                    CommonRefactoringUtil.htmlEmphasize(targetModule.name)
                )
                conflicts.putValue(target, CommonRefactoringUtil.capitalize(message))
                referencesToSkip += refExpr
            }
        }
        internalUsages.removeIf { it.reference?.element?.let { it in referencesToSkip } ?: false }
    }

    fun checkVisibilityInUsages(usages: Collection<UsageInfo>, conflicts: MultiMap<PsiElement, String>) {
        val declarationToContainers = HashMap<KtNamedDeclaration, MutableSet<PsiElement>>()
        for (usage in usages) {
            val element = usage.element
            if (element == null || usage !is MoveRenameUsageInfo || usage is NonCodeUsageInfo) continue

            if (isToBeMoved(element!!)) continue

            val referencedElement = (usage as MoveRenameUsageInfo).referencedElement?.namedUnwrappedElement as? KtNamedDeclaration ?: continue
            val referencedDescriptor = resolutionFacade.resolveToDescriptor(referencedElement)

            if (referencedDescriptor is DeclarationDescriptorWithVisibility
                && (referencedDescriptor as DeclarationDescriptorWithVisibility).visibility == Visibilities.PUBLIC
                && moveTarget is KotlinMoveTargetForExistingElement
                && (moveTarget as KotlinMoveTargetForExistingElement).targetElement.parentsWithSelf.filterIsInstance<KtClassOrObject>().all { it.isPublic }
            ) continue

            val container = element!!.getUsageContext()
            if (!declarationToContainers.getOrPut(referencedElement) { HashSet<PsiElement>() }.add(container)) continue

            val targetContainer = moveTarget.getContainerDescriptor() ?: continue

            val referencingDescriptor = when (container) {
                is KtDeclaration -> (container as KtDeclaration).unsafeResolveToDescriptor()
                is PsiMember -> (container as PsiMember).getJavaMemberDescriptor()
                else -> null
            } ?: continue
            val actualVisibility = if (referencingDescriptor.isJavaDescriptor) referencedDescriptor.visibilityAsViewedFromJava() else null
            val originalDescriptorToCheck = referencedDescriptor.wrap(newVisibility = actualVisibility) ?: referencedDescriptor
            val newDescriptorToCheck = referencedDescriptor.asPredicted(targetContainer, actualVisibility) ?: continue

            if (originalDescriptorToCheck.isVisibleIn(referencingDescriptor) && !newDescriptorToCheck.isVisibleIn(referencingDescriptor)) {
                val message = "${render(container)} uses ${render(referencedElement)} which will be inaccessible after move"
                conflicts.putValue(element, message.capitalize())
            }
        }
    }

    fun checkVisibilityInDeclarations(conflicts: MultiMap<PsiElement, String>) {
        val targetContainer = moveTarget.getContainerDescriptor() ?: return

        fun DeclarationDescriptor.targetAwareContainingDescriptor(): DeclarationDescriptor? {
            val defaultContainer = containingDeclaration
            val psi = (this as? DeclarationDescriptorWithSource)?.source?.getPsi()
            return if (psi != null && psi!! in allElementsToMove) targetContainer else defaultContainer
        }

        fun DeclarationDescriptor.targetAwareContainers(): Sequence<DeclarationDescriptor> {
            return generateSequence(this) { it.targetAwareContainingDescriptor() }.drop(1)
        }

        fun DeclarationDescriptor.targetAwareContainingClass(): ClassDescriptor? {
            return targetAwareContainers().firstIsInstanceOrNull<ClassDescriptor>()
        }

        fun DeclarationDescriptorWithVisibility.isProtectedVisible(referrerDescriptor: DeclarationDescriptor): Boolean {
            val givenClassDescriptor = targetAwareContainingClass()
            val referrerClassDescriptor = referrerDescriptor.targetAwareContainingClass() ?: return false
            if (givenClassDescriptor != null && givenClassDescriptor!!.isCompanionObject) {
                val companionOwner = givenClassDescriptor!!.targetAwareContainingClass()
                if (companionOwner != null && referrerClassDescriptor.isSubclassOf(companionOwner!!)) return true
            }
            val whatDeclaration = DescriptorUtils.unwrapFakeOverrideToAnyDeclaration(this)
            val classDescriptor = whatDeclaration.targetAwareContainingClass() ?: return false
            if (referrerClassDescriptor.isSubclassOf(classDescriptor)) return true
            return referrerDescriptor.targetAwareContainingDescriptor()?.let { isProtectedVisible(it) } ?: false
        }

        fun DeclarationDescriptorWithVisibility.isVisibleFrom(ref: PsiReference): Boolean {
            val targetVisibility = visibility.normalize()
            if (targetVisibility == Visibilities.PUBLIC) return true

            val refElement = ref.element
            val referrer = refElement.getStrictParentOfType<KtNamedDeclaration>()
            var referrerDescriptor = referrer?.resolveToDescriptorIfAny() ?: return true
            if (referrerDescriptor is ClassDescriptor && refElement.getParentOfTypeAndBranch<KtSuperTypeListEntry> { typeReference } != null) {
                (referrerDescriptor as ClassDescriptor).unsubstitutedPrimaryConstructor?.let { referrerDescriptor = it }
            }

            if (!isVisibleIn(referrerDescriptor)) return true

            return when (targetVisibility) {
                Visibilities.PROTECTED -> isProtectedVisible(referrerDescriptor)
                else -> isVisibleIn(targetContainer)
            }
        }

        for (declaration in elementsToMove - doNotGoIn) {
            declaration.forEachDescendantOfType<KtReferenceExpression> { refExpr ->
                refExpr.references
                    .forEach { ref ->
                        val target = ref.resolve() ?: return@forEach
                        if (isToBeMoved(target)) return@forEach

                        val targetDescriptor = when (target) {
                            is KtDeclaration -> (target as KtDeclaration).unsafeResolveToDescriptor()
                            is PsiMember -> (target as PsiMember).getJavaMemberDescriptor()
                            else -> null
                        } as? DeclarationDescriptorWithVisibility ?: return@forEach

                        var isVisible = targetDescriptor.isVisibleFrom(ref)
                        if (isVisible && targetDescriptor is ConstructorDescriptor) {
                            isVisible = (targetDescriptor as ConstructorDescriptor).containingDeclaration.isVisibleFrom(ref)
                        }

                        if (!isVisible) {
                            val message = "${render(declaration)} uses ${render(target)} which will be inaccessible after move"
                            conflicts.putValue(refExpr, message.capitalize())
                        }
                    }
            }
        }
    }

    private fun isToBeMoved(element: PsiElement): Boolean = allElementsToMove.any { it.isAncestor(element, false) }

    private fun checkInternalMemberUsages(conflicts: MultiMap<PsiElement, String>) {
        val targetModule = moveTarget.getTargetModule(project) ?: return

        val membersToCheck = LinkedHashSet<KtDeclaration>()
        val memberCollector = object : KtVisitorVoid() {
            override fun visitClassOrObject(classOrObject: KtClassOrObject) {
                val declarations = classOrObject.declarations
                declarations.filterTo(membersToCheck) { it.hasModifier(KtTokens.INTERNAL_KEYWORD) }
                declarations.forEach { it.accept(this) }
            }
        }
        elementsToMove.forEach { it.accept(memberCollector) }

        for (memberToCheck in membersToCheck) {
            for (reference in ReferencesSearch.search(memberToCheck)) {
                val element = reference.element ?: continue
                val usageModule = ModuleUtilCore.findModuleForPsiElement(element) ?: continue
                if (usageModule != targetModule && targetModule !in usageModule.implementedModules && !isToBeMoved(element)) {
                    val container = element.getUsageContext()
                    val message = "${render(container)} uses internal ${render(memberToCheck)} which will be inaccessible after move"
                    conflicts.putValue(element, message.capitalize())
                }
            }
        }
    }

    private fun checkSealedClassMove(conflicts: MultiMap<PsiElement, String>) {
        val visited = HashSet<PsiElement>()
        for (elementToMove in elementsToMove) {
            if (!visited.add(elementToMove)) continue
            if (elementToMove !is KtClassOrObject) continue

            val rootClass: KtClass
            val rootClassDescriptor: ClassDescriptor
            if (elementToMove is KtClass && (elementToMove as KtClass).isSealed()) {
                rootClass = elementToMove as KtClass
                rootClassDescriptor = rootClass.resolveToDescriptorIfAny() ?: return
            } else {
                val classDescriptor = (elementToMove as KtClassOrObject).resolveToDescriptorIfAny() ?: return
                val superClassDescriptor = classDescriptor.getSuperClassNotAny() ?: return
                if (superClassDescriptor.modality != Modality.SEALED) return
                rootClassDescriptor = superClassDescriptor
                rootClass = rootClassDescriptor.source.getPsi() as? KtClass ?: return
            }

            val subclasses = rootClassDescriptor.sealedSubclasses.mapNotNull { it.source.getPsi() }
            if (subclasses.isEmpty()) continue

            visited.add(rootClass)
            visited.addAll(subclasses)

            if (isToBeMoved(rootClass) && subclasses.all { isToBeMoved(it) }) continue

            val message = if (elementToMove == rootClass) {
                "Sealed class '${rootClass.name}' must be moved with all its subclasses"
            } else {
                val type = ElementDescriptionUtil.getElementDescription(elementToMove, UsageViewTypeLocation.INSTANCE).capitalize()
                "$type '${rootClass.name}' must be moved with sealed parent class and all its subclasses"
            }
            conflicts.putValue(elementToMove, message)
        }
    }

    private fun checkNameClashes(conflicts: MultiMap<PsiElement, String>) {
        fun <T> equivalent(a: T, b: T): Boolean = when (a) {
            is DeclarationDescriptor -> when (a as T) {
                is FunctionDescriptor -> b is FunctionDescriptor
                        && equivalent((a as FunctionDescriptor).name, (b as FunctionDescriptor).name) && (a as FunctionDescriptor).valueParameters.zip(
                    (b as FunctionDescriptor).valueParameters).all { equivalent(it.first, it.second) }
                else -> b is DeclarationDescriptor && equivalent((a as DeclarationDescriptor).name, (b as DeclarationDescriptor).name)
            }
            is Name -> b is Name && (a as Name).asString() == (b as Name).asString()
            is FqName -> b is Name && (a as FqName).asString() == (b as Name).asString()
            is ValueParameterDescriptor -> b is ValueParameterDescriptor
                    && equivalent((a as ValueParameterDescriptor).type, (b as ValueParameterDescriptor).type)
            is KotlinType -> {
                if (b !is KotlinType) false
                else {
                    val aSupertypes = (a as KotlinType).constructor.supertypesWithAny()
                    val bSupertypes = (b as KotlinType).constructor.supertypesWithAny()
                    when {
                        (a as KotlinType).isAnyOrNullableAny() && (b as KotlinType).isAnyOrNullableAny() ->         // a = T(?) | Any(?), b = T(?) | Any(?)
                            true                                                    // => 100% clash
                        aSupertypes.size == 1 && bSupertypes.size == 1 ->           // a = T: T1, b = T: T2
                            equivalent(aSupertypes.first(), bSupertypes.first())    // equivalent(T1, T2) => clash
                        (a as KotlinType).arguments.size != 0 && (b as KotlinType).arguments.size != 0 ->
                            equivalent(                                             // a = Something<....>, b = SomethingElse<....>
                                (a as KotlinType).constructor.declarationDescriptor?.name,          // equivalent(Something, SomethingElse) => clash
                                (b as KotlinType).constructor.declarationDescriptor?.name
                            )
                        else -> a == b                                              // common case. a == b => clash
                    }
                }
            }
            else -> false
        }

        fun walkDeclarations(
            currentScopeDeclaration: DeclarationDescriptor,
            declaration: DeclarationDescriptor,
            report: (String, String) -> Unit
        ) {
            fun descrText(d: DeclarationDescriptor) = d.findPsi()?.text ?: "unknown"
            when (currentScopeDeclaration) {
                is PackageFragmentDescriptor -> {
                    fun getPackage(decl: PackageFragmentDescriptor) = decl.containingDeclaration.getPackage(decl.fqName)
                    val packageDescriptor = getPackage(currentScopeDeclaration as PackageFragmentDescriptor)
                    if ((declaration.containingDeclaration)?.fqNameOrNull() == currentScopeDeclaration.fqNameOrNull()) {
                        return
                    }
                    packageDescriptor
                        .memberScope
                        .getContributedDescriptors { it == declaration.name }
                        .filter { equivalent(it, declaration) }
                        .forEach { report(descrText(it), packageDescriptor.fqName.asString()) }
                    return
                }
                is ClassDescriptor -> {
                    if ((declaration.containingDeclaration)?.fqNameOrNull() != currentScopeDeclaration.fqNameOrNull()) {
                        (currentScopeDeclaration as ClassDescriptor)
                            .unsubstitutedMemberScope
                            .getContributedDescriptors { it == declaration.name }
                            .filter { equivalent(it, declaration) }
                            .forEach { report(descrText(it), descrText(currentScopeDeclaration)) }
                    }
                }

            }
            currentScopeDeclaration.containingDeclaration?.let { walkDeclarations(it, declaration, report) }
        }

        (elementsToMove - doNotGoIn)
            .filter {it is PsiNamedElement}
            .forEach { declaration ->
                val declarationDescriptor =
                    declaration.analyze().get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration)
                if (declarationDescriptor is DeclarationDescriptor) {
                    val baseDescriptor = moveTarget.getContainerDescriptor()
                    if (baseDescriptor != null) {
                        walkDeclarations(baseDescriptor!!, declarationDescriptor!!) { conflictingDecl, scopeName ->
                            conflicts.putValue(
                                declaration,
                                "Following declarations would clash: \"${declaration.getText()}\" (element to move) " +
                                        "and \"\n$conflictingDecl\" declared in scope " +
                                        "$scopeName (move destination)"
                            )
                        }
                    }
                }
            }
    }

    fun checkAllConflicts(
        externalUsages: MutableSet<UsageInfo>,
        internalUsages: MutableSet<UsageInfo>,
        conflicts: MultiMap<PsiElement, String>
    ) {
        checkModuleConflictsInUsages(externalUsages, conflicts)
        checkModuleConflictsInDeclarations(internalUsages, conflicts)
        checkVisibilityInUsages(externalUsages, conflicts)
        checkVisibilityInDeclarations(conflicts)
        checkInternalMemberUsages(conflicts)
        checkSealedClassMove(conflicts)
        checkNameClashes(conflicts)
    }
}

fun analyzeConflictsInFile(
    file: KtFile,
    usages: Collection<UsageInfo>,
    moveTarget: KotlinMoveTarget,
    allElementsToMove: Collection<PsiElement>,
    conflicts: MultiMap<PsiElement, String>,
    onUsageUpdate: (List<UsageInfo>) -> Unit
) {
    val elementsToMove = file.declarations
    if (elementsToMove.isEmpty()) return

    val (internalUsages, externalUsages) = usages.partition { it is KotlinMoveUsage && (it as KotlinMoveUsage).isInternal }
    val internalUsageSet = internalUsages.toMutableSet()
    val externalUsageSet = externalUsages.toMutableSet()

    val conflictChecker = MoveConflictChecker(
        file.project,
        elementsToMove,
        moveTarget,
        elementsToMove.first(),
        allElementsToMove = allElementsToMove
    )
    conflictChecker.checkAllConflicts(externalUsageSet, internalUsageSet, conflicts)

    if (externalUsageSet.size != externalUsages.size || internalUsageSet.size != internalUsages.size) {
        onUsageUpdate((externalUsageSet + internalUsageSet).toList())
    }
}
