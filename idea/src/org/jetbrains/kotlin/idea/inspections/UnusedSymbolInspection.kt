/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.daemon.QuickFixBundle
import com.intellij.codeInsight.daemon.impl.analysis.HighlightUtil
import com.intellij.codeInsight.daemon.impl.analysis.JavaHighlightUtil
import com.intellij.codeInspection.*
import com.intellij.codeInspection.deadCode.UnusedDeclarationInspection
import com.intellij.codeInspection.ex.EntryPointsManager
import com.intellij.codeInspection.ex.EntryPointsManagerBase
import com.intellij.codeInspection.ex.EntryPointsManagerImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.PsiSearchHelper.SearchCostResult
import com.intellij.psi.search.PsiSearchHelper.SearchCostResult.*
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.DefinitionsScopedSearch
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.safeDelete.SafeDeleteHandler
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.project.implementingDescriptors
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.isInheritable
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.toDescriptor
import org.jetbrains.kotlin.idea.findUsages.KotlinFindUsagesHandlerFactory
import org.jetbrains.kotlin.idea.findUsages.handlers.KotlinFindClassUsagesHandler
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.inspections.collections.isCalling
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.intentions.isFinalizeMethod
import org.jetbrains.kotlin.idea.isMainFunction
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.quickfix.RemoveUnusedFunctionParameterFix
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.references.resolveMainReferenceToDescriptors
import org.jetbrains.kotlin.idea.search.findScriptsWithUsages
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchOptions
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchParameters
import org.jetbrains.kotlin.idea.search.isCheapEnoughToSearchConsideringOperators
import org.jetbrains.kotlin.idea.search.projectScope
import org.jetbrains.kotlin.idea.search.usagesSearch.dataClassComponentFunction
import org.jetbrains.kotlin.idea.search.usagesSearch.getAccessorNames
import org.jetbrains.kotlin.idea.search.usagesSearch.getClassNameForCompanionObject
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.util.hasActualsFor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.isInlineClassType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.util.findCallableMemberBySignature
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.util.*
import javax.swing.JComponent
import javax.swing.JPanel

class UnusedSymbolInspection : AbstractKotlinInspection() {
    companion object {
        private val javaInspection = UnusedDeclarationInspection()

        private val KOTLIN_ADDITIONAL_ANNOTATIONS = listOf("kotlin.test.*")

        private val KOTLIN_BUILTIN_ENUM_FUNCTIONS = listOf(FqName("kotlin.enumValues"), FqName("kotlin.enumValueOf"))

        private val ENUM_STATIC_METHODS = listOf("values", "valueOf")

        private fun KtDeclaration.hasKotlinAdditionalAnnotation() =
            this is KtNamedDeclaration && checkAnnotatedUsingPatterns(this, KOTLIN_ADDITIONAL_ANNOTATIONS)

        fun isEntryPoint(declaration: KtNamedDeclaration): Boolean {
            if (declaration.hasKotlinAdditionalAnnotation()) return true
            if (declaration is KtClass && declaration.declarations.any { it.hasKotlinAdditionalAnnotation() }) return true

            // Some of the main-function-cases are covered by 'javaInspection.isEntryPoint(lightElement)' call
            // but not all of them: light method for parameterless main still points to parameterless name
            // that is not an actual entry point from Java language point of view
            if (declaration.isMainFunction()) return true

            val lightElement: PsiElement? = when (declaration) {
                is KtClassOrObject -> declaration.toLightClass()
                is KtNamedFunction, is KtSecondaryConstructor -> LightClassUtil.getLightClassMethod(declaration as KtFunction)
                is KtProperty, is KtParameter -> {
                    if (declaration is KtParameter && !declaration.hasValOrVar()) return false
                    // can't rely on light element, check annotation ourselves
                    val entryPointsManager = EntryPointsManager.getInstance(declaration.project) as EntryPointsManagerBase
                    return checkAnnotatedUsingPatterns(
                        declaration,
                        entryPointsManager.additionalAnnotations + entryPointsManager.ADDITIONAL_ANNOTATIONS
                    )
                }
                else -> return false
            }

            if (lightElement == null) return false

            if (isCheapEnoughToSearchUsages(declaration) == TOO_MANY_OCCURRENCES) return false

            return javaInspection.isEntryPoint(lightElement)
        }

        private fun isCheapEnoughToSearchUsages(declaration: KtNamedDeclaration): SearchCostResult {
            val project = declaration.project
            val psiSearchHelper = PsiSearchHelper.getInstance(project)

            val usedScripts = findScriptsWithUsages(declaration)
            if (usedScripts.isNotEmpty()) {
                if (!ScriptConfigurationManager.getInstance(declaration.project).updater.ensureConfigurationUpToDate(usedScripts)) {
                    return TOO_MANY_OCCURRENCES
                }
            }

            val useScope = psiSearchHelper.getUseScope(declaration)
            if (useScope is GlobalSearchScope) {
                var zeroOccurrences = true
                for (name in listOf(declaration.name) + declaration.getAccessorNames() + listOfNotNull(declaration.getClassNameForCompanionObject())) {
                    if (name == null) continue
                    when (psiSearchHelper.isCheapEnoughToSearchConsideringOperators(name, useScope, null, null)) {
                        ZERO_OCCURRENCES -> {
                        } // go on, check other names
                        FEW_OCCURRENCES -> zeroOccurrences = false
                        TOO_MANY_OCCURRENCES -> return TOO_MANY_OCCURRENCES // searching usages is too expensive; behave like it is used
                    }
                }

                if (zeroOccurrences) return ZERO_OCCURRENCES
            }
            return FEW_OCCURRENCES
        }

        private fun KtProperty.isSerializationImplicitlyUsedField(): Boolean {
            val ownerObject = getNonStrictParentOfType<KtClassOrObject>()
            if (ownerObject is KtObjectDeclaration && ownerObject.isCompanion()) {
                val lightClass = ownerObject.getNonStrictParentOfType<KtClass>()?.toLightClass() ?: return false
                return lightClass.fields.any { it.name == name && HighlightUtil.isSerializationImplicitlyUsedField(it) }
            }
            return false
        }

        private fun KtNamedFunction.isSerializationImplicitlyUsedMethod(): Boolean =
            toLightMethods().any { JavaHighlightUtil.isSerializationRelatedMethod(it, it.containingClass) }

        // variation of IDEA's AnnotationUtil.checkAnnotatedUsingPatterns()
        fun checkAnnotatedUsingPatterns(
            declaration: KtNamedDeclaration,
            annotationPatterns: Collection<String>
        ): Boolean {
            if (declaration.annotationEntries.isEmpty()) return false
            val context = declaration.analyze()
            val annotationsPresent = declaration.annotationEntries.mapNotNull {
                context[BindingContext.ANNOTATION, it]?.fqName?.asString()
            }
            if (annotationsPresent.isEmpty()) return false

            for (pattern in annotationPatterns) {
                val hasAnnotation = if (pattern.endsWith(".*")) {
                    annotationsPresent.any { it.startsWith(pattern.dropLast(1)) }
                } else {
                    pattern in annotationsPresent
                }
                if (hasAnnotation) return true
            }

            return false
        }
    }

    override fun runForWholeFile() = true

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return namedDeclarationVisitor(fun(declaration) {
            val message = declaration.describe()?.let { "$it is never used" } ?: return

            if (!ProjectRootsUtil.isInProjectSource(declaration)) return

            // Simple PSI-based checks
            if (declaration is KtObjectDeclaration && declaration.isCompanion()) return // never mark companion object as unused (there are too many reasons it can be needed for)

            if (declaration is KtSecondaryConstructor && declaration.containingClass()?.isEnum() == true) return
            if (declaration.hasModifier(KtTokens.OVERRIDE_KEYWORD)) return
            if (declaration is KtProperty && declaration.isLocal) return
            if (declaration is KtParameter &&
                (declaration.getParent().parent !is KtPrimaryConstructor || !declaration.hasValOrVar())
            ) return

            // More expensive, resolve-based checks
            val descriptor = declaration.resolveToDescriptorIfAny() ?: return
            if (descriptor is FunctionDescriptor && descriptor.isOperator) return
            if (isEntryPoint(declaration)) return
            if (declaration.isFinalizeMethod(descriptor)) return
            if (declaration is KtProperty && declaration.isSerializationImplicitlyUsedField()) return
            if (declaration is KtNamedFunction && declaration.isSerializationImplicitlyUsedMethod()) return
            // properties can be referred by component1/component2, which is too expensive to search, don't mark them as unused
            if (declaration is KtParameter && declaration.dataClassComponentFunction() != null) return
            // experimental annotations
            if (descriptor is ClassDescriptor && descriptor.kind == ClassKind.ANNOTATION_CLASS) {
                val fqName = descriptor.fqNameSafe.asString()
                val languageVersionSettings = declaration.languageVersionSettings
                if (fqName in languageVersionSettings.getFlag(AnalysisFlags.experimental) ||
                    fqName in languageVersionSettings.getFlag(AnalysisFlags.useExperimental)
                ) return
            }

            // Main checks: finding reference usages && text usages
            if (hasNonTrivialUsages(declaration, descriptor)) return
            if (declaration is KtClassOrObject && classOrObjectHasTextUsages(declaration)) return

            val psiElement = declaration.nameIdentifier ?: (declaration as? KtConstructor<*>)?.getConstructorKeyword() ?: return
            val problemDescriptor = holder.manager.createProblemDescriptor(
                psiElement,
                null,
                message,
                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                true,
                *createQuickFixes(declaration).toTypedArray()
            )

            holder.registerProblem(problemDescriptor)
        })
    }

    override val suppressionKey: String get() = "unused"

    private fun classOrObjectHasTextUsages(classOrObject: KtClassOrObject): Boolean {
        var hasTextUsages = false

        // Finding text usages
        if (classOrObject.useScope is GlobalSearchScope) {
            val findClassUsagesHandler = KotlinFindClassUsagesHandler(classOrObject, KotlinFindUsagesHandlerFactory(classOrObject.project))
            findClassUsagesHandler.processUsagesInText(
                classOrObject,
                { hasTextUsages = true; false },
                GlobalSearchScope.projectScope(classOrObject.project)
            )
        }

        return hasTextUsages
    }

    private fun hasNonTrivialUsages(declaration: KtNamedDeclaration, descriptor: DeclarationDescriptor? = null): Boolean {
        val project = declaration.project
        val psiSearchHelper = PsiSearchHelper.getInstance(project)

        val useScope = psiSearchHelper.getUseScope(declaration)
        val restrictedScope = if (useScope is GlobalSearchScope) {
            val enoughToSearchUsages = isCheapEnoughToSearchUsages(declaration)
            val zeroOccurrences = when (enoughToSearchUsages) {
                ZERO_OCCURRENCES -> true
                FEW_OCCURRENCES -> false
                TOO_MANY_OCCURRENCES -> return true // searching usages is too expensive; behave like it is used
            }

            if (zeroOccurrences && !declaration.hasActualModifier()) {
                if (declaration is KtObjectDeclaration && declaration.isCompanion()) {
                    // go on: companion object can be used only in containing class
                } else {
                    return false
                }
            }
            if (declaration.hasActualModifier()) {
                KotlinSourceFilterScope.projectSources(project.projectScope(), project)
            } else {
                KotlinSourceFilterScope.projectSources(useScope, project)
            }
        } else useScope

        if (declaration is KtTypeParameter) {
            val containingClass = declaration.containingClass()
            if (containingClass != null) {
                val isOpenClass = containingClass.isInterface()
                        || containingClass.hasModifier(KtTokens.ABSTRACT_KEYWORD)
                        || containingClass.hasModifier(KtTokens.SEALED_KEYWORD)
                        || containingClass.hasModifier(KtTokens.OPEN_KEYWORD)
                if (isOpenClass && hasOverrides(containingClass, restrictedScope)) return true
            }
        }

        return (declaration is KtObjectDeclaration && declaration.isCompanion() &&
                declaration.body?.declarations?.isNotEmpty() == true) ||
                hasReferences(declaration, descriptor, restrictedScope) ||
                hasOverrides(declaration, restrictedScope) ||
                hasFakeOverrides(declaration, restrictedScope) ||
                hasPlatformImplementations(declaration, descriptor)
    }

    private fun checkDeclaration(declaration: KtNamedDeclaration, importedDeclaration: KtNamedDeclaration): Boolean =
        declaration !in importedDeclaration.parentsWithSelf && !hasNonTrivialUsages(importedDeclaration)

    private val KtNamedDeclaration.isObjectOrEnum: Boolean get() = this is KtObjectDeclaration || this is KtClass && isEnum()

    private fun checkReference(ref: PsiReference, declaration: KtNamedDeclaration, descriptor: DeclarationDescriptor?): Boolean {
        if (declaration.isAncestor(ref.element)) return true // usages inside element's declaration are not counted

        if (ref.element.parent is KtValueArgumentName) return true // usage of parameter in form of named argument is not counted

        val import = ref.element.getParentOfType<KtImportDirective>(false)
        if (import != null) {
            if (import.aliasName != null && import.aliasName != declaration.name) {
                return false
            }
            // check if we import member(s) from object / nested object / enum and search for their usages
            val originalDeclaration = (descriptor as? TypeAliasDescriptor)?.classDescriptor?.findPsi() as? KtNamedDeclaration
            if (declaration is KtClassOrObject || originalDeclaration is KtClassOrObject) {
                if (import.isAllUnder) {
                    val importedFrom = import.importedReference?.getQualifiedElementSelector()?.mainReference?.resolve()
                            as? KtClassOrObject ?: return true
                    return importedFrom.declarations.none { it is KtNamedDeclaration && hasNonTrivialUsages(it) }
                } else {
                    if (import.importedFqName != declaration.fqName) {
                        val importedDeclaration =
                            import.importedReference?.getQualifiedElementSelector()?.mainReference?.resolve() as? KtNamedDeclaration
                                ?: return true

                        if (declaration.isObjectOrEnum || importedDeclaration.containingClassOrObject is KtObjectDeclaration) return checkDeclaration(
                            declaration,
                            importedDeclaration
                        )

                        if (originalDeclaration?.isObjectOrEnum == true) return checkDeclaration(
                            originalDeclaration,
                            importedDeclaration
                        )

                        // check type alias
                        if (importedDeclaration.fqName == declaration.fqName) return true
                    }
                }
            }
            return true
        }

        return false
    }

    private fun hasReferences(
        declaration: KtNamedDeclaration,
        descriptor: DeclarationDescriptor?,
        useScope: SearchScope
    ): Boolean {
        fun checkReference(ref: PsiReference): Boolean = checkReference(ref, declaration, descriptor)

        val searchOptions = KotlinReferencesSearchOptions(acceptCallableOverrides = declaration.hasActualModifier())
        val searchParameters = KotlinReferencesSearchParameters(
            declaration,
            useScope,
            kotlinOptions = searchOptions
        )
        val referenceUsed: Boolean by lazy { !ReferencesSearch.search(searchParameters).forEach(::checkReference) }

        if (descriptor is FunctionDescriptor && DescriptorUtils.findJvmNameAnnotation(descriptor) != null) {
            if (referenceUsed) return true
        }

        if (declaration is KtCallableDeclaration && declaration.canBeHandledByLightMethods(descriptor)) {
            val lightMethods = declaration.toLightMethods()
            if (lightMethods.isNotEmpty()) {
                val lightMethodsUsed = lightMethods.any { method ->
                    !MethodReferencesSearch.search(method).forEach(::checkReference)
                }
                if (lightMethodsUsed) return true
                if (!declaration.hasActualModifier()) return false
            }
        }

        if (declaration is KtEnumEntry) {
            val enumClass = declaration.containingClass()?.takeIf { it.isEnum() }
            if (enumClass != null && ReferencesSearch.search(
                    KotlinReferencesSearchParameters(
                        enumClass,
                        useScope,
                        kotlinOptions = searchOptions
                    )
                ).any(::hasBuiltInEnumFunctionReference)
            ) return true
        }

        return referenceUsed || checkPrivateDeclaration(declaration, descriptor)
    }

    private fun hasBuiltInEnumFunctionReference(reference: PsiReference): Boolean {
        return when (val parent = reference.element.getParentOfTypes(
            true,
            KtTypeReference::class.java,
            KtQualifiedExpression::class.java,
            KtCallableReferenceExpression::class.java
        )) {
            is KtTypeReference -> {
                val target = (parent.getStrictParentOfType<KtTypeArgumentList>() ?: parent)
                    .getParentOfTypes(true, KtCallExpression::class.java, KtCallableDeclaration::class.java)
                when (target) {
                    is KtCallExpression -> target.isCalling(KOTLIN_BUILTIN_ENUM_FUNCTIONS)
                    is KtCallableDeclaration -> {
                        target.anyDescendantOfType<KtCallExpression> {
                            val context = it.analyze(BodyResolveMode.PARTIAL_WITH_CFA)
                            it.isCalling(KOTLIN_BUILTIN_ENUM_FUNCTIONS, context) && it.isUsedAsExpression(context)
                        }
                    }
                    else -> false
                }
            }
            is KtQualifiedExpression -> parent.callExpression?.calleeExpression?.text in ENUM_STATIC_METHODS
            is KtCallableReferenceExpression -> parent.callableReference.text in ENUM_STATIC_METHODS
            else -> false
        }
    }

    private fun checkPrivateDeclaration(declaration: KtNamedDeclaration, descriptor: DeclarationDescriptor?): Boolean {
        if (descriptor == null || !declaration.isPrivateNestedClassOrObject) return false

        val set = hashSetOf<KtSimpleNameExpression>()
        declaration.containingKtFile.importList?.acceptChildren(simpleNameExpressionRecursiveVisitor {
            set += it
        })

        return set.mapNotNull { it.referenceExpression() }
            .filter { descriptor in it.resolveMainReferenceToDescriptors() }
            .any { !checkReference(it.mainReference, declaration, descriptor) }
    }

    private fun KtCallableDeclaration.canBeHandledByLightMethods(descriptor: DeclarationDescriptor?): Boolean {
        return when {
            descriptor is ConstructorDescriptor -> {
                val classDescriptor = descriptor.constructedClass
                !classDescriptor.isInline && classDescriptor.visibility != Visibilities.LOCAL
            }
            hasModifier(KtTokens.INTERNAL_KEYWORD) -> false
            descriptor !is FunctionDescriptor -> true
            else -> !descriptor.hasInlineClassParameters()
        }
    }

    private fun FunctionDescriptor.hasInlineClassParameters(): Boolean {
        return when {
            dispatchReceiverParameter?.type?.isInlineClassType() == true -> true
            extensionReceiverParameter?.type?.isInlineClassType() == true -> true
            else -> valueParameters.any { it.type.isInlineClassType() }
        }
    }

    private fun hasOverrides(declaration: KtNamedDeclaration, useScope: SearchScope): Boolean =
        DefinitionsScopedSearch.search(declaration, useScope).findFirst() != null

    private fun hasFakeOverrides(declaration: KtNamedDeclaration, useScope: SearchScope): Boolean {
        val ownerClass = declaration.containingClassOrObject as? KtClass ?: return false
        if (!ownerClass.isInheritable()) return false
        val descriptor = declaration.toDescriptor() as? CallableMemberDescriptor ?: return false
        if (descriptor.modality == Modality.ABSTRACT) return false
        val lightMethods = declaration.toLightMethods()
        return DefinitionsScopedSearch.search(ownerClass, useScope).any { element: PsiElement ->

            when (element) {
                is KtLightClass -> {
                    val memberBySignature =
                        (element.kotlinOrigin?.toDescriptor() as? ClassDescriptor)?.findCallableMemberBySignature(descriptor)
                    memberBySignature != null &&
                            !memberBySignature.kind.isReal &&
                            memberBySignature.overriddenDescriptors.any { it != descriptor }
                }
                is PsiClass ->
                    lightMethods.any { lightMethod ->

                        val sameMethods = element.findMethodsBySignature(lightMethod, true)
                        sameMethods.all { it.containingClass != element } &&
                                sameMethods.any { it.containingClass != lightMethod.containingClass }
                    }
                else ->
                    false
            }
        }
    }

    private fun hasPlatformImplementations(declaration: KtNamedDeclaration, descriptor: DeclarationDescriptor?): Boolean {
        if (!declaration.hasExpectModifier()) return false

        if (descriptor !is MemberDescriptor) return false
        val commonModuleDescriptor = declaration.containingKtFile.findModuleDescriptor()

        return commonModuleDescriptor.implementingDescriptors.any { it.hasActualsFor(descriptor) } ||
                commonModuleDescriptor.hasActualsFor(descriptor)
    }

    override fun createOptionsPanel(): JComponent? {
        val panel = JPanel(GridBagLayout())
        panel.add(
            EntryPointsManagerImpl.createConfigureAnnotationsButton(),
            GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, Insets(0, 0, 0, 0), 0, 0)
        )
        return panel
    }

    private fun createQuickFixes(declaration: KtNamedDeclaration): List<LocalQuickFix> {
        val list = ArrayList<LocalQuickFix>()

        list.add(SafeDeleteFix(declaration))

        for (annotationEntry in declaration.annotationEntries) {
            val typeElement = annotationEntry.typeReference?.typeElement as? KtUserType ?: continue
            val target = typeElement.referenceExpression?.resolveMainReferenceToDescriptors()?.singleOrNull() ?: continue
            val fqName = target.importableFqName?.asString() ?: continue

            // checks taken from com.intellij.codeInspection.util.SpecialAnnotationsUtilBase.createAddToSpecialAnnotationFixes
            if (fqName.startsWith("kotlin.")
                || fqName.startsWith("java.")
                || fqName.startsWith("javax.")
                || fqName.startsWith("org.jetbrains.annotations.")
            )
                continue

            val intentionAction = createAddToDependencyInjectionAnnotationsFix(declaration.project, fqName)

            list.add(IntentionWrapper(intentionAction, declaration.containingFile))
        }

        return list
    }
}

class SafeDeleteFix(declaration: KtDeclaration) : LocalQuickFix {
    private val name: String =
        if (declaration is KtConstructor<*>) "Safe delete constructor"
        else QuickFixBundle.message("safe.delete.text", declaration.name)

    override fun getName() = name

    override fun getFamilyName() = "Safe delete"

    override fun startInWriteAction(): Boolean = false

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val declaration = descriptor.psiElement.getStrictParentOfType<KtDeclaration>() ?: return
        if (!FileModificationService.getInstance().prepareFileForWrite(declaration.containingFile)) return
        if (declaration is KtParameter && declaration.parent is KtParameterList && declaration.parent?.parent is KtFunction) {
            RemoveUnusedFunctionParameterFix(declaration).invoke(project, declaration.findExistingEditor(), declaration.containingKtFile)
        } else {
            ApplicationManager.getApplication().invokeLater(
                { safeDelete(project, declaration) },
                ModalityState.NON_MODAL
            )
        }
    }
}

private fun safeDelete(project: Project, declaration: PsiElement) {
    SafeDeleteHandler.invoke(project, arrayOf(declaration), false)
}