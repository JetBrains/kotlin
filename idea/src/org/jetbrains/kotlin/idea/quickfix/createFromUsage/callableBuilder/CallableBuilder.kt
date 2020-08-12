/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder

import com.intellij.codeInsight.daemon.impl.quickfix.CreateFromUsageUtils
import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.codeInsight.template.*
import com.intellij.codeInsight.template.impl.TemplateImpl
import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.UnfairTextRange
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import org.jetbrains.kotlin.cfg.pseudocode.getContainingPseudocode
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.MutablePackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.idea.FrontendInternals
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithAllCompilerChecks
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaClassDescriptor
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.core.util.CodeInsightUtils
import org.jetbrains.kotlin.idea.core.util.isMultiLine
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.ClassKind
import org.jetbrains.kotlin.idea.refactoring.*
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.idea.util.DialogWithEditor
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.psi.typeRefHelpers.setReceiverTypeReference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeImpl
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.resolve.scopes.utils.memberScopeAsImportingScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.util.*
import kotlin.math.max

/**
 * Represents a single choice for a type (e.g. parameter type or return type).
 */
class TypeCandidate(val theType: KotlinType, scope: HierarchicalScope? = null) {
    val typeParameters: Array<TypeParameterDescriptor>
    var renderedTypes: List<String> = emptyList()
        private set
    var renderedTypeParameters: List<RenderedTypeParameter>? = null
        private set

    fun render(typeParameterNameMap: Map<TypeParameterDescriptor, String>, fakeFunction: FunctionDescriptor?) {
        renderedTypes = theType.renderShort(typeParameterNameMap)
        renderedTypeParameters = typeParameters.map {
            RenderedTypeParameter(it, it.containingDeclaration == fakeFunction, typeParameterNameMap.getValue(it))
        }
    }

    init {
        val typeParametersInType = theType.getTypeParameters()
        if (scope == null) {
            typeParameters = typeParametersInType.toTypedArray()
            renderedTypes = theType.renderShort(Collections.emptyMap())
        } else {
            typeParameters = getTypeParameterNamesNotInScope(typeParametersInType, scope).toTypedArray()
        }
    }

    override fun toString() = theType.toString()
}

data class RenderedTypeParameter(
    val typeParameter: TypeParameterDescriptor,
    val fake: Boolean,
    val text: String
)

fun List<TypeCandidate>.getTypeByRenderedType(renderedTypes: List<String>): KotlinType? =
    firstOrNull { it.renderedTypes == renderedTypes }?.theType

class CallableBuilderConfiguration(
    val callableInfos: List<CallableInfo>,
    val originalElement: KtElement,
    val currentFile: KtFile = originalElement.containingKtFile,
    val currentEditor: Editor? = null,
    val isExtension: Boolean = false,
    val enableSubstitutions: Boolean = true
)

sealed class CallablePlacement {
    class WithReceiver(val receiverTypeCandidate: TypeCandidate) : CallablePlacement()
    class NoReceiver(val containingElement: PsiElement) : CallablePlacement()
}

class CallableBuilder(val config: CallableBuilderConfiguration) {
    private var finished: Boolean = false

    val currentFileContext = config.currentFile.analyzeWithContent()

    private lateinit var _currentFileModule: ModuleDescriptor
    val currentFileModule: ModuleDescriptor
        get() {
            if (!_currentFileModule.isValid) {
                updateCurrentModule()
            }
            return _currentFileModule
        }

    init {
        updateCurrentModule()
    }

    val pseudocode: Pseudocode? by lazy { config.originalElement.getContainingPseudocode(currentFileContext) }

    private val typeCandidates = HashMap<TypeInfo, List<TypeCandidate>>()

    var placement: CallablePlacement? = null

    private val elementsToShorten = ArrayList<KtElement>()

    private fun updateCurrentModule() {
        _currentFileModule = config.currentFile.analyzeWithAllCompilerChecks().moduleDescriptor
    }

    fun computeTypeCandidates(typeInfo: TypeInfo): List<TypeCandidate> =
        typeCandidates.getOrPut(typeInfo) { typeInfo.getPossibleTypes(this).map { TypeCandidate(it) } }

    private fun computeTypeCandidates(
        typeInfo: TypeInfo,
        substitutions: List<KotlinTypeSubstitution>,
        scope: HierarchicalScope
    ): List<TypeCandidate> {
        if (!typeInfo.substitutionsAllowed) return computeTypeCandidates(typeInfo)
        return typeCandidates.getOrPut(typeInfo) {
            val types = typeInfo.getPossibleTypes(this).asReversed()

            // We have to use semantic equality here
            data class EqWrapper(val _type: KotlinType) {
                override fun equals(other: Any?) = this === other
                        || other is EqWrapper && KotlinTypeChecker.DEFAULT.equalTypes(_type, other._type)

                override fun hashCode() = 0 // no good way to compute hashCode() that would agree with our equals()
            }

            val newTypes = LinkedHashSet(types.map(::EqWrapper))
            for (substitution in substitutions) {
                // each substitution can be applied or not, so we offer all options
                val toAdd = newTypes.map { it._type.substitute(substitution, typeInfo.variance) }
                // substitution.byType are type arguments, but they cannot already occur in the type before substitution
                val toRemove = newTypes.filter { substitution.byType in it._type }

                newTypes.addAll(toAdd.map(::EqWrapper))
                newTypes.removeAll(toRemove)
            }

            if (newTypes.isEmpty()) {
                newTypes.add(EqWrapper(currentFileModule.builtIns.anyType))
            }

            newTypes.map { TypeCandidate(it._type, scope) }.asReversed()
        }
    }

    private fun buildNext(iterator: Iterator<CallableInfo>) {
        if (iterator.hasNext()) {
            val context = Context(iterator.next())
            runWriteAction { context.buildAndRunTemplate { buildNext(iterator) } }
            ApplicationManager.getApplication().invokeLater { context.showDialogIfNeeded() }
        } else {
            runWriteAction { ShortenReferences.DEFAULT.process(elementsToShorten) }
        }
    }

    fun build(onFinish: () -> Unit = {}) {
        try {
            assert(config.currentEditor != null) { "Can't run build() without editor" }
            check(!finished) { "Current builder has already finished" }
            buildNext(config.callableInfos.iterator())
        } finally {
            finished = true
            onFinish()
        }
    }

    private inner class Context(val callableInfo: CallableInfo) {
        val skipReturnType: Boolean
        val jetFileToEdit: KtFile
        val containingFileEditor: Editor
        val containingElement: PsiElement
        val dialogWithEditor: DialogWithEditor?
        val receiverClassDescriptor: ClassifierDescriptor?
        val typeParameterNameMap: Map<TypeParameterDescriptor, String>
        val receiverTypeCandidate: TypeCandidate?
        val mandatoryTypeParametersAsCandidates: List<TypeCandidate>
        val substitutions: List<KotlinTypeSubstitution>
        var finished: Boolean = false

        init {
            // gather relevant information

            val placement = placement
            var nullableReceiver = false
            when (placement) {
                is CallablePlacement.NoReceiver -> {
                    containingElement = placement.containingElement
                    receiverClassDescriptor = with(placement.containingElement) {
                        when (this) {
                            is KtClassOrObject -> currentFileContext[BindingContext.CLASS, this]
                            is PsiClass -> getJavaClassDescriptor()
                            else -> null
                        }
                    }
                }
                is CallablePlacement.WithReceiver -> {
                    val theType = placement.receiverTypeCandidate.theType
                    nullableReceiver = theType.isMarkedNullable
                    receiverClassDescriptor = theType.constructor.declarationDescriptor
                    val classDeclaration = receiverClassDescriptor?.let { DescriptorToSourceUtils.getSourceFromDescriptor(it) }
                    containingElement = if (!config.isExtension && classDeclaration != null) classDeclaration else config.currentFile
                }
                else -> throw IllegalArgumentException("Placement wan't initialized")
            }
            val receiverType = receiverClassDescriptor?.defaultType?.let {
                if (nullableReceiver) it.makeNullable() else it
            }

            val project = config.currentFile.project

            if (containingElement.containingFile != config.currentFile) {
                NavigationUtil.activateFileWithPsiElement(containingElement)
            }

            dialogWithEditor = if (containingElement is KtElement) {
                jetFileToEdit = containingElement.containingKtFile
                containingFileEditor = if (jetFileToEdit != config.currentFile) {
                    FileEditorManager.getInstance(project).selectedTextEditor!!
                } else {
                    config.currentEditor!!
                }
                null
            } else {
                val dialog = object : DialogWithEditor(project, KotlinBundle.message("fix.create.from.usage.dialog.title"), "") {
                    override fun doOKAction() {
                        project.executeWriteCommand(KotlinBundle.message("premature.end.of.template")) {
                            TemplateManagerImpl.getTemplateState(editor)?.gotoEnd(false)
                        }
                        super.doOKAction()
                    }
                }
                containingFileEditor = dialog.editor
                with(containingFileEditor.settings) {
                    additionalColumnsCount = config.currentEditor!!.settings.getRightMargin(project)
                    additionalLinesCount = 5
                }
                jetFileToEdit = PsiDocumentManager.getInstance(project).getPsiFile(containingFileEditor.document) as KtFile
                jetFileToEdit.analysisContext = config.currentFile
                dialog
            }

            val scope = getDeclarationScope()

            receiverTypeCandidate = receiverType?.let { TypeCandidate(it, scope) }

            val fakeFunction: FunctionDescriptor?
            // figure out type substitutions for type parameters
            val substitutionMap = LinkedHashMap<KotlinType, KotlinType>()
            if (config.enableSubstitutions) {
                collectSubstitutionsForReceiverTypeParameters(receiverType, substitutionMap)
                val typeArgumentsForFakeFunction = callableInfo.typeParameterInfos
                    .map {
                        val typeCandidates = computeTypeCandidates(it)
                        assert(typeCandidates.size == 1) { "Ambiguous type candidates for type parameter $it: $typeCandidates" }
                        typeCandidates.first().theType
                    }
                    .subtract(substitutionMap.keys)
                fakeFunction = createFakeFunctionDescriptor(scope, typeArgumentsForFakeFunction.size)
                collectSubstitutionsForCallableTypeParameters(fakeFunction, typeArgumentsForFakeFunction, substitutionMap)
                mandatoryTypeParametersAsCandidates = listOfNotNull(receiverTypeCandidate) + typeArgumentsForFakeFunction.map {
                    TypeCandidate(substitutionMap[it]!!, scope)
                }
            } else {
                fakeFunction = null
                mandatoryTypeParametersAsCandidates = Collections.emptyList()
            }
            substitutions = substitutionMap.map { KotlinTypeSubstitution(it.key, it.value) }

            callableInfo.parameterInfos.forEach {
                computeTypeCandidates(it.typeInfo, substitutions, scope)
            }

            val returnTypeCandidate = computeTypeCandidates(callableInfo.returnTypeInfo, substitutions, scope).singleOrNull()
            skipReturnType = when (callableInfo.kind) {
                CallableKind.FUNCTION ->
                    returnTypeCandidate?.theType?.isUnit() ?: false
                CallableKind.CLASS_WITH_PRIMARY_CONSTRUCTOR ->
                    callableInfo.returnTypeInfo == TypeInfo.Empty || returnTypeCandidate?.theType?.isAnyOrNullableAny() ?: false
                CallableKind.CONSTRUCTOR -> true
                CallableKind.PROPERTY -> containingElement is KtBlockExpression
            }

            // figure out type parameter renames to avoid conflicts
            typeParameterNameMap = getTypeParameterRenames(scope)
            callableInfo.parameterInfos.forEach { renderTypeCandidates(it.typeInfo, typeParameterNameMap, fakeFunction) }
            if (!skipReturnType) {
                renderTypeCandidates(callableInfo.returnTypeInfo, typeParameterNameMap, fakeFunction)
            }
            receiverTypeCandidate?.render(typeParameterNameMap, fakeFunction)
            mandatoryTypeParametersAsCandidates.forEach { it.render(typeParameterNameMap, fakeFunction) }
        }

        private fun getDeclarationScope(): HierarchicalScope {
            if (config.isExtension || receiverClassDescriptor == null) {
                return currentFileModule.getPackage(config.currentFile.packageFqName).memberScope.memberScopeAsImportingScope()
            }

            if (receiverClassDescriptor is ClassDescriptorWithResolutionScopes) {
                return receiverClassDescriptor.scopeForMemberDeclarationResolution
            }

            assert(receiverClassDescriptor is JavaClassDescriptor) { "Unexpected receiver class: $receiverClassDescriptor" }

            val projections = ((receiverClassDescriptor as JavaClassDescriptor).declaredTypeParameters)
                .map { TypeProjectionImpl(it.defaultType) }
            val memberScope = receiverClassDescriptor.getMemberScope(projections)

            return LexicalScopeImpl(
                memberScope.memberScopeAsImportingScope(), receiverClassDescriptor, false, null,
                LexicalScopeKind.SYNTHETIC
            ) {
                receiverClassDescriptor.typeConstructor.parameters.forEach { addClassifierDescriptor(it) }
            }
        }

        private fun collectSubstitutionsForReceiverTypeParameters(
            receiverType: KotlinType?,
            result: MutableMap<KotlinType, KotlinType>
        ) {
            if (placement is CallablePlacement.NoReceiver) return

            val classTypeParameters = receiverType?.arguments ?: Collections.emptyList()
            val ownerTypeArguments = (placement as? CallablePlacement.WithReceiver)?.receiverTypeCandidate?.theType?.arguments
                ?: Collections.emptyList()
            assert(ownerTypeArguments.size == classTypeParameters.size)
            ownerTypeArguments.zip(classTypeParameters).forEach { result[it.first.type] = it.second.type }
        }

        private fun collectSubstitutionsForCallableTypeParameters(
            fakeFunction: FunctionDescriptor,
            typeArguments: Set<KotlinType>,
            result: MutableMap<KotlinType, KotlinType>
        ) {
            for ((typeArgument, typeParameter) in typeArguments.zip(fakeFunction.typeParameters)) {
                result[typeArgument] = typeParameter.defaultType
            }
        }

        @OptIn(FrontendInternals::class)
        private fun createFakeFunctionDescriptor(scope: HierarchicalScope, typeParameterCount: Int): FunctionDescriptor {
            val fakeFunction = SimpleFunctionDescriptorImpl.create(
                MutablePackageFragmentDescriptor(currentFileModule, FqName("fake")),
                Annotations.EMPTY,
                Name.identifier("fake"),
                CallableMemberDescriptor.Kind.SYNTHESIZED,
                SourceElement.NO_SOURCE
            )

            val validator = CollectingNameValidator { scope.findClassifier(Name.identifier(it), NoLookupLocation.FROM_IDE) == null }
            val parameterNames = KotlinNameSuggester.suggestNamesForTypeParameters(typeParameterCount, validator)
            val typeParameters = (0 until typeParameterCount).map {
                TypeParameterDescriptorImpl.createWithDefaultBound(
                    fakeFunction,
                    Annotations.EMPTY,
                    false,
                    Variance.INVARIANT,
                    Name.identifier(parameterNames[it]),
                    it,
                    jetFileToEdit.getResolutionFacade().frontendService()
                )
            }

            return fakeFunction.initialize(
                null, null, typeParameters, Collections.emptyList(), null,
                null, Visibilities.INTERNAL
            )
        }

        private fun renderTypeCandidates(
            typeInfo: TypeInfo,
            typeParameterNameMap: Map<TypeParameterDescriptor, String>,
            fakeFunction: FunctionDescriptor?
        ) {
            typeCandidates[typeInfo]?.forEach { it.render(typeParameterNameMap, fakeFunction) }
        }

        private fun isInsideInnerOrLocalClass(): Boolean {
            val classOrObject = containingElement.getNonStrictParentOfType<KtClassOrObject>()
            return classOrObject is KtClass && (classOrObject.isInner() || classOrObject.isLocal)
        }

        private fun createDeclarationSkeleton(): KtNamedDeclaration {
            with(config) {
                val assignmentToReplace =
                    if (containingElement is KtBlockExpression && (callableInfo as? PropertyInfo)?.writable == true) {
                        originalElement as KtBinaryExpression
                    } else null
                val pointerOfAssignmentToReplace = assignmentToReplace?.createSmartPointer()

                val ownerTypeString = if (isExtension) {
                    val renderedType = receiverTypeCandidate!!.renderedTypes.first()
                    val isFunctionType = receiverTypeCandidate.theType.constructor.declarationDescriptor is FunctionClassDescriptor
                    if (isFunctionType) "($renderedType)." else "$renderedType."
                } else ""

                val classKind = (callableInfo as? ClassWithPrimaryConstructorInfo)?.classInfo?.kind

                fun renderParamList(): String {
                    val prefix = if (classKind == ClassKind.ANNOTATION_CLASS) "val " else ""
                    val list = callableInfo.parameterInfos.indices.joinToString(", ") { i -> "${prefix}p$i: Any" }
                    return if (callableInfo.parameterInfos.isNotEmpty()
                        || callableInfo.kind == CallableKind.FUNCTION
                        || callableInfo.kind == CallableKind.CONSTRUCTOR
                    ) "($list)" else list
                }

                val paramList = when (callableInfo.kind) {
                    CallableKind.FUNCTION, CallableKind.CLASS_WITH_PRIMARY_CONSTRUCTOR, CallableKind.CONSTRUCTOR ->
                        renderParamList()
                    CallableKind.PROPERTY -> ""
                }
                val returnTypeString = if (skipReturnType || assignmentToReplace != null) "" else ": Any"
                val header = "$ownerTypeString${callableInfo.name.quoteIfNeeded()}$paramList$returnTypeString"

                val psiFactory = KtPsiFactory(currentFile)

                val modifiers = buildString {
                    val modifierList = callableInfo.modifierList?.copied() ?: psiFactory.createEmptyModifierList()
                    val visibilityKeyword = modifierList.visibilityModifierType()
                    if (visibilityKeyword == null) {
                        val defaultVisibility =
                            if (callableInfo.isAbstract) ""
                            else if (containingElement is KtClassOrObject
                                && !(containingElement is KtClass && containingElement.isInterface())
                                && containingElement.isAncestor(config.originalElement)
                                && callableInfo.kind != CallableKind.CONSTRUCTOR
                            ) "private "
                            else if (isExtension) "private "
                            else ""
                        append(defaultVisibility)
                    }

                    // TODO: Get rid of isAbstract
                    if (callableInfo.isAbstract
                        && containingElement is KtClass
                        && !containingElement.isInterface()
                        && !modifierList.hasModifier(KtTokens.ABSTRACT_KEYWORD)
                    ) {
                        modifierList.appendModifier(KtTokens.ABSTRACT_KEYWORD)
                    }

                    val text = modifierList.normalize().text
                    if (text.isNotEmpty()) {
                        append("$text ")
                    }
                }

                val isExpectClassMember by lazy {
                    containingElement is KtClassOrObject && containingElement.resolveToDescriptorIfAny()?.isExpect ?: false
                }

                val declaration: KtNamedDeclaration = when (callableInfo.kind) {
                    CallableKind.FUNCTION, CallableKind.CONSTRUCTOR -> {
                        val body = when {
                            callableInfo is ConstructorInfo -> if (callableInfo.withBody) "{\n\n}" else ""
                            callableInfo.isAbstract -> ""
                            containingElement is KtClass && containingElement.hasModifier(KtTokens.EXTERNAL_KEYWORD) -> ""
                            containingElement is KtObjectDeclaration && containingElement.hasModifier(KtTokens.EXTERNAL_KEYWORD) -> ""
                            containingElement is KtObjectDeclaration && containingElement.isCompanion()
                                    && containingElement.parent.parent is KtClass
                                    && (containingElement.parent.parent as KtClass).hasModifier(KtTokens.EXTERNAL_KEYWORD) -> ""
                            isExpectClassMember -> ""
                            else -> "{\n\n}"

                        }
                        @Suppress("USELESS_CAST") // KT-10755
                        when {
                            callableInfo is FunctionInfo -> psiFactory.createFunction("${modifiers}fun<> $header $body") as KtNamedDeclaration
                            (callableInfo as ConstructorInfo).isPrimary -> {
                                val constructorText = if (modifiers.isNotEmpty()) "${modifiers}constructor$paramList" else paramList
                                psiFactory.createPrimaryConstructor(constructorText) as KtNamedDeclaration
                            }
                            else -> psiFactory.createSecondaryConstructor("${modifiers}constructor$paramList $body") as KtNamedDeclaration
                        }
                    }
                    CallableKind.CLASS_WITH_PRIMARY_CONSTRUCTOR -> {
                        val classWithPrimaryConstructorInfo = callableInfo as ClassWithPrimaryConstructorInfo
                        with(classWithPrimaryConstructorInfo.classInfo) {
                            val classBody = when (kind) {
                                ClassKind.ANNOTATION_CLASS, ClassKind.ENUM_ENTRY -> ""
                                else -> "{\n\n}"
                            }
                            val safeName = name.quoteIfNeeded()
                            when (kind) {
                                ClassKind.ENUM_ENTRY -> {
                                    val targetParent = applicableParents.singleOrNull()
                                    if (!(targetParent is KtClass && targetParent.isEnum())) throw AssertionError("Enum class expected: ${targetParent?.text}")
                                    val hasParameters = targetParent.primaryConstructorParameters.isNotEmpty()
                                    psiFactory.createEnumEntry("$safeName${if (hasParameters) "()" else " "}")
                                }
                                else -> {
                                    val openMod = if (open && kind != ClassKind.INTERFACE) "open " else ""
                                    val innerMod = if (inner || isInsideInnerOrLocalClass()) "inner " else ""
                                    val typeParamList = when (kind) {
                                        ClassKind.PLAIN_CLASS, ClassKind.INTERFACE -> "<>"
                                        else -> ""
                                    }
                                    val ctor =
                                        classWithPrimaryConstructorInfo.primaryConstructorVisibility?.name?.let { " $it constructor" } ?: ""
                                    psiFactory.createDeclaration<KtClassOrObject>(
                                        "$openMod$innerMod${kind.keyword} $safeName$typeParamList$ctor$paramList$returnTypeString $classBody"
                                    )
                                }
                            }
                        }
                    }
                    CallableKind.PROPERTY -> {
                        val isVar = (callableInfo as PropertyInfo).writable
                        val valVar = if (isVar) "var" else "val"
                        val accessors = if (isExtension && !isExpectClassMember) {
                            buildString {
                                append("\nget() {}")
                                if (isVar) {
                                    append("\nset() {}")
                                }
                            }
                        } else ""
                        psiFactory.createProperty("$modifiers$valVar<> $header$accessors")
                    }
                }

                val newInitializer = pointerOfAssignmentToReplace?.element
                if (newInitializer != null) {
                    (declaration as KtProperty).initializer = newInitializer.right
                    return newInitializer.replace(declaration) as KtCallableDeclaration
                }

                val container = if (containingElement is KtClass && callableInfo.isForCompanion) {
                    containingElement.getOrCreateCompanionObject()
                } else containingElement
                val declarationInPlace = placeDeclarationInContainer(declaration, container, config.originalElement, jetFileToEdit)

                if (declarationInPlace is KtSecondaryConstructor) {
                    val containingClass = declarationInPlace.containingClassOrObject!!
                    val primaryConstructorParameters = containingClass.primaryConstructorParameters
                    if (primaryConstructorParameters.isNotEmpty()) {
                        declarationInPlace.replaceImplicitDelegationCallWithExplicit(true)
                    } else if ((receiverClassDescriptor as ClassDescriptor).getSuperClassOrAny().constructors
                            .all { it.valueParameters.isNotEmpty() }
                    ) {
                        declarationInPlace.replaceImplicitDelegationCallWithExplicit(false)
                    }
                    if (declarationInPlace.valueParameters.size > primaryConstructorParameters.size) {
                        val hasCompatibleTypes = primaryConstructorParameters.zip(callableInfo.parameterInfos).all { (primary, secondary) ->
                            val primaryType = currentFileContext[BindingContext.TYPE, primary.typeReference] ?: return@all false
                            val secondaryType = computeTypeCandidates(secondary.typeInfo).firstOrNull()?.theType ?: return@all false
                            secondaryType.isSubtypeOf(primaryType)
                        }
                        if (hasCompatibleTypes) {
                            val delegationCallArgumentList = declarationInPlace.getDelegationCall().valueArgumentList
                            primaryConstructorParameters.forEach {
                                val name = it.name
                                if (name != null) delegationCallArgumentList?.addArgument(psiFactory.createArgument(name))
                            }
                        }
                    }
                }

                return declarationInPlace
            }
        }

        private fun getTypeParameterRenames(scope: HierarchicalScope): Map<TypeParameterDescriptor, String> {
            val allTypeParametersNotInScope = LinkedHashSet<TypeParameterDescriptor>()

            mandatoryTypeParametersAsCandidates.asSequence()
                .plus(callableInfo.parameterInfos.asSequence().flatMap { typeCandidates[it.typeInfo]!!.asSequence() })
                .flatMap { it.typeParameters.asSequence() }
                .toCollection(allTypeParametersNotInScope)

            if (!skipReturnType) {
                computeTypeCandidates(callableInfo.returnTypeInfo).asSequence().flatMapTo(allTypeParametersNotInScope) {
                    it.typeParameters.asSequence()
                }
            }

            val validator = CollectingNameValidator { scope.findClassifier(Name.identifier(it), NoLookupLocation.FROM_IDE) == null }
            val typeParameterNames = allTypeParametersNotInScope.map {
                KotlinNameSuggester.suggestNameByName(it.name.asString(), validator)
            }

            return allTypeParametersNotInScope.zip(typeParameterNames).toMap()
        }

        private fun setupTypeReferencesForShortening(
            declaration: KtNamedDeclaration,
            parameterTypeExpressions: List<TypeExpression>
        ) {
            if (config.isExtension) {
                val receiverTypeText = receiverTypeCandidate!!.theType.renderLong(typeParameterNameMap).first()
                val replacingTypeRef = KtPsiFactory(declaration).createType(receiverTypeText)
                (declaration as KtCallableDeclaration).setReceiverTypeReference(replacingTypeRef)!!
            }

            val returnTypeRefs = declaration.getReturnTypeReferences()
            if (returnTypeRefs.isNotEmpty()) {
                val returnType = typeCandidates[callableInfo.returnTypeInfo]!!.getTypeByRenderedType(
                    returnTypeRefs.map { it.text }
                )
                if (returnType != null) {
                    // user selected a given type
                    replaceWithLongerName(returnTypeRefs, returnType)
                }
            }

            val valueParameters = declaration.getValueParameters()
            val parameterIndicesToShorten = ArrayList<Int>()
            assert(valueParameters.size == parameterTypeExpressions.size)
            for ((i, parameter) in valueParameters.asSequence().withIndex()) {
                val parameterTypeRef = parameter.typeReference
                if (parameterTypeRef != null) {
                    val parameterType = parameterTypeExpressions[i].typeCandidates.getTypeByRenderedType(
                        listOf(parameterTypeRef.text)
                    )
                    if (parameterType != null) {
                        replaceWithLongerName(listOf(parameterTypeRef), parameterType)
                        parameterIndicesToShorten.add(i)
                    }
                }
            }
        }

        private fun postprocessDeclaration(declaration: KtNamedDeclaration) {
            if (callableInfo is PropertyInfo && callableInfo.isLateinitPreferred) {
                if (declaration.containingClassOrObject == null) return
                val propertyDescriptor = declaration.resolveToDescriptorIfAny() as? PropertyDescriptor ?: return
                val returnType = propertyDescriptor.returnType ?: return
                if (TypeUtils.isNullableType(returnType) || KotlinBuiltIns.isPrimitiveType(returnType)) return
                declaration.addModifier(KtTokens.LATEINIT_KEYWORD)
            }

            if (callableInfo.isAbstract) {
                val containingClass = declaration.containingClassOrObject
                if (containingClass is KtClass && containingClass.isInterface()) {
                    declaration.removeModifier(KtTokens.ABSTRACT_KEYWORD)
                }
            }
        }

        private fun setupDeclarationBody(func: KtDeclarationWithBody) {
            if (func !is KtNamedFunction && func !is KtPropertyAccessor) return
            if (skipReturnType && callableInfo is FunctionInfo && callableInfo.preferEmptyBody) return
            val oldBody = func.bodyExpression ?: return
            val bodyText = getFunctionBodyTextFromTemplate(
                func.project,
                TemplateKind.FUNCTION,
                if (callableInfo.name.isNotEmpty()) callableInfo.name else null,
                if (skipReturnType) "Unit" else (func as? KtFunction)?.typeReference?.text ?: "",
                receiverClassDescriptor?.importableFqName ?: receiverClassDescriptor?.name?.let { FqName.topLevel(it) }
            )
            oldBody.replace(KtPsiFactory(func).createBlock(bodyText))
        }

        private fun setupCallTypeArguments(callElement: KtCallElement, typeParameters: List<TypeParameterDescriptor>) {
            val oldTypeArgumentList = callElement.typeArgumentList ?: return
            val renderedTypeArgs = typeParameters.map { typeParameter ->
                val type = substitutions.first { it.byType.constructor.declarationDescriptor == typeParameter }.forType
                IdeDescriptorRenderers.SOURCE_CODE.renderType(type)
            }
            if (renderedTypeArgs.isEmpty()) {
                oldTypeArgumentList.delete()
            } else {
                oldTypeArgumentList.replace(KtPsiFactory(callElement).createTypeArguments(renderedTypeArgs.joinToString(", ", "<", ">")))
                elementsToShorten.add(callElement.typeArgumentList!!)
            }
        }

        private fun setupReturnTypeTemplate(builder: TemplateBuilder, declaration: KtNamedDeclaration): TypeExpression? {
            val candidates = typeCandidates[callableInfo.returnTypeInfo]!!
            if (candidates.isEmpty()) return null

            val elementToReplace: KtElement?
            val expression: TypeExpression = when (declaration) {
                is KtCallableDeclaration -> {
                    elementToReplace = declaration.typeReference
                    TypeExpression.ForTypeReference(candidates)
                }
                is KtClassOrObject -> {
                    elementToReplace = declaration.superTypeListEntries.firstOrNull()
                    TypeExpression.ForDelegationSpecifier(candidates)
                }
                else -> throw AssertionError("Unexpected declaration kind: ${declaration.text}")
            }
            if (elementToReplace == null) return null

            if (candidates.size == 1) {
                builder.replaceElement(elementToReplace, (expression.calculateResult(null) as TextResult).text)
                return null
            }

            builder.replaceElement(elementToReplace, expression)
            return expression
        }

        private fun setupValVarTemplate(builder: TemplateBuilder, property: KtProperty) {
            if (!(callableInfo as PropertyInfo).writable) {
                builder.replaceElement(property.valOrVarKeyword, ValVarExpression)
            }
        }

        private fun setupTypeParameterListTemplate(
            builder: TemplateBuilderImpl,
            declaration: KtNamedDeclaration
        ): TypeParameterListExpression? {
            when (declaration) {
                is KtObjectDeclaration -> return null
                !is KtTypeParameterListOwner -> throw AssertionError("Unexpected declaration kind: ${declaration.text}")
            }

            val typeParameterList = (declaration as KtTypeParameterListOwner).typeParameterList ?: return null

            val typeParameterMap = HashMap<String, List<RenderedTypeParameter>>()

            val mandatoryTypeParameters = ArrayList<RenderedTypeParameter>()
            //receiverTypeCandidate?.let { mandatoryTypeParameters.addAll(it.renderedTypeParameters!!) }
            mandatoryTypeParametersAsCandidates.asSequence().flatMapTo(mandatoryTypeParameters) { it.renderedTypeParameters!!.asSequence() }

            callableInfo.parameterInfos.asSequence()
                .flatMap { typeCandidates[it.typeInfo]!!.asSequence() }
                .forEach { typeParameterMap[it.renderedTypes.first()] = it.renderedTypeParameters!! }

            if (declaration.getReturnTypeReference() != null) {
                typeCandidates[callableInfo.returnTypeInfo]!!.forEach {
                    typeParameterMap[it.renderedTypes.first()] = it.renderedTypeParameters!!
                }
            }

            val expression = TypeParameterListExpression(
                mandatoryTypeParameters,
                typeParameterMap,
                callableInfo.kind != CallableKind.CLASS_WITH_PRIMARY_CONSTRUCTOR
            )
            val leftSpace = typeParameterList.prevSibling as? PsiWhiteSpace
            val rangeStart = leftSpace?.startOffset ?: typeParameterList.startOffset
            val offset = typeParameterList.startOffset
            val range = UnfairTextRange(rangeStart - offset, typeParameterList.endOffset - offset)
            builder.replaceElement(typeParameterList, range, "TYPE_PARAMETER_LIST", expression, false)
            return expression
        }

        private fun setupParameterTypeTemplates(builder: TemplateBuilder, parameterList: List<KtParameter>): List<TypeExpression> {
            assert(parameterList.size == callableInfo.parameterInfos.size)

            val typeParameters = ArrayList<TypeExpression>()
            for ((parameter, jetParameter) in callableInfo.parameterInfos.zip(parameterList)) {
                val parameterTypeExpression = TypeExpression.ForTypeReference(typeCandidates[parameter.typeInfo]!!)
                val parameterTypeRef = jetParameter.typeReference!!
                builder.replaceElement(parameterTypeRef, parameterTypeExpression)

                // add parameter name to the template
                val possibleNamesFromExpression = parameter.typeInfo.getPossibleNamesFromExpression(currentFileContext)
                val possibleNames = arrayOf(*parameter.nameSuggestions.toTypedArray(), *possibleNamesFromExpression)

                // figure out suggested names for each type option
                val parameterTypeToNamesMap = HashMap<String, Array<String>>()
                typeCandidates[parameter.typeInfo]!!.forEach { typeCandidate ->
                    val suggestedNames = KotlinNameSuggester.suggestNamesByType(typeCandidate.theType, { true })
                    parameterTypeToNamesMap[typeCandidate.renderedTypes.first()] = suggestedNames.toTypedArray()
                }

                // add expression to builder
                val parameterNameExpression = ParameterNameExpression(possibleNames, parameterTypeToNamesMap)
                val parameterNameIdentifier = jetParameter.nameIdentifier!!
                builder.replaceElement(parameterNameIdentifier, parameterNameExpression)

                typeParameters.add(parameterTypeExpression)
            }
            return typeParameters
        }

        private fun replaceWithLongerName(typeRefs: List<KtTypeReference>, theType: KotlinType) {
            val psiFactory = KtPsiFactory(jetFileToEdit.project)
            val fullyQualifiedReceiverTypeRefs = theType.renderLong(typeParameterNameMap).map { psiFactory.createType(it) }
            (typeRefs zip fullyQualifiedReceiverTypeRefs).forEach { (shortRef, longRef) -> shortRef.replace(longRef) }
        }

        private fun transformToJavaMemberIfApplicable(declaration: KtNamedDeclaration): Boolean {
            fun convertToJava(targetClass: PsiClass): PsiMember? {
                val psiFactory = KtPsiFactory(declaration)

                psiFactory.createPackageDirectiveIfNeeded(config.currentFile.packageFqName)?.let {
                    declaration.containingFile.addBefore(it, null)
                }

                val adjustedDeclaration = when (declaration) {
                    is KtNamedFunction, is KtProperty -> {
                        val klass = psiFactory.createClass("class Foo {}")
                        klass.body!!.add(declaration)
                        (declaration.replace(klass) as KtClass).body!!.declarations.first()
                    }
                    else -> declaration
                }

                return when (adjustedDeclaration) {
                    is KtNamedFunction, is KtSecondaryConstructor -> {
                        createJavaMethod(adjustedDeclaration as KtFunction, targetClass)
                    }
                    is KtProperty -> {
                        createJavaField(adjustedDeclaration, targetClass)
                    }
                    is KtClass -> {
                        createJavaClass(adjustedDeclaration, targetClass)
                    }
                    else -> null
                }
            }

            if (config.isExtension || receiverClassDescriptor !is JavaClassDescriptor) return false

            val targetClass = DescriptorToSourceUtils.getSourceFromDescriptor(receiverClassDescriptor) as? PsiClass
            if (targetClass == null || !targetClass.canRefactor()) return false

            val project = declaration.project

            val newJavaMember = convertToJava(targetClass) ?: return false

            val modifierList = newJavaMember.modifierList!!
            if (newJavaMember is PsiMethod || newJavaMember is PsiClass) {
                modifierList.setModifierProperty(PsiModifier.FINAL, false)
            }

            val needStatic = when (callableInfo) {
                is ClassWithPrimaryConstructorInfo -> with(callableInfo.classInfo) {
                    !inner && kind != ClassKind.ENUM_ENTRY && kind != ClassKind.ENUM_CLASS
                }
                else -> callableInfo.receiverTypeInfo.staticContextRequired
            }
            modifierList.setModifierProperty(PsiModifier.STATIC, needStatic)

            JavaCodeStyleManager.getInstance(project).shortenClassReferences(newJavaMember)

            val descriptor = OpenFileDescriptor(project, targetClass.containingFile.virtualFile)
            val targetEditor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true)!!

            when (newJavaMember) {
                is PsiMethod -> CreateFromUsageUtils.setupEditor(newJavaMember, targetEditor)
                is PsiField -> targetEditor.caretModel.moveToOffset(newJavaMember.endOffset - 1)
                is PsiClass -> {
                    val constructor = newJavaMember.constructors.firstOrNull()
                    val superStatement = constructor?.body?.statements?.firstOrNull() as? PsiExpressionStatement
                    val superCall = superStatement?.expression as? PsiMethodCallExpression
                    if (superCall != null) {
                        val lParen = superCall.argumentList.firstChild
                        targetEditor.caretModel.moveToOffset(lParen.endOffset)
                    } else {
                        targetEditor.caretModel.moveToOffset(newJavaMember.nameIdentifier?.startOffset ?: newJavaMember.startOffset)
                    }
                }
            }
            targetEditor.scrollingModel.scrollToCaret(ScrollType.RELATIVE)

            return true
        }

        private fun setupEditor(declaration: KtNamedDeclaration) {
            if (declaration is KtProperty && !declaration.hasInitializer() && containingElement is KtBlockExpression) {
                val defaultValueType = typeCandidates[callableInfo.returnTypeInfo]!!.firstOrNull()?.theType
                val defaultValue = defaultValueType?.let { CodeInsightUtils.defaultInitializer(it) } ?: "null"
                val initializer = declaration.setInitializer(KtPsiFactory(declaration).createExpression(defaultValue))!!
                val range = initializer.textRange
                containingFileEditor.selectionModel.setSelection(range.startOffset, range.endOffset)
                containingFileEditor.caretModel.moveToOffset(range.endOffset)
                return
            }
            if (declaration is KtSecondaryConstructor && !declaration.hasImplicitDelegationCall()) {
                containingFileEditor.caretModel.moveToOffset(declaration.getDelegationCall().valueArgumentList!!.startOffset + 1)
                return
            }
            setupEditorSelection(containingFileEditor, declaration)
        }

        // build templates
        fun buildAndRunTemplate(onFinish: () -> Unit) {
            val declarationSkeleton = createDeclarationSkeleton()
            val project = declarationSkeleton.project
            val declarationPointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(declarationSkeleton)

            // build templates
            PsiDocumentManager.getInstance(project).commitAllDocuments()
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(containingFileEditor.document)

            val caretModel = containingFileEditor.caretModel
            caretModel.moveToOffset(jetFileToEdit.node.startOffset)

            val declaration = declarationPointer.element ?: return

            val declarationMarker = containingFileEditor.document.createRangeMarker(declaration.textRange)

            val builder = TemplateBuilderImpl(jetFileToEdit)
            if (declaration is KtProperty) {
                setupValVarTemplate(builder, declaration)
            }
            if (!skipReturnType) {
                setupReturnTypeTemplate(builder, declaration)
            }

            val parameterTypeExpressions = setupParameterTypeTemplates(builder, declaration.getValueParameters())

            // add a segment for the parameter list
            // Note: because TemplateBuilderImpl does not have a replaceElement overload that takes in both a TextRange and alwaysStopAt, we
            // need to create the segment first and then hack the Expression into the template later. We use this template to update the type
            // parameter list as the user makes selections in the parameter types, and we need alwaysStopAt to be false so the user can't tab to
            // it.
            val expression = setupTypeParameterListTemplate(builder, declaration)

            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(containingFileEditor.document)
            // the template built by TemplateBuilderImpl is ordered by element position, but we want types to be first, so hack it
            val templateImpl = builder.buildInlineTemplate() as TemplateImpl
            val variables = templateImpl.variables!!
            if (variables.isNotEmpty()) {
                val typeParametersVar = if (expression != null) variables.removeAt(0) else null
                for (i in callableInfo.parameterInfos.indices) {
                    Collections.swap(variables, i * 2, i * 2 + 1)
                }
                typeParametersVar?.let { variables.add(it) }
            }

            // TODO: Disabled shortening names because it causes some tests fail. Refactor code to use automatic reference shortening
            templateImpl.isToShortenLongNames = false

            // run the template
            TemplateManager.getInstance(project).startTemplate(containingFileEditor, templateImpl, object : TemplateEditingAdapter() {
                private fun finishTemplate(brokenOff: Boolean) {
                    try {
                        PsiDocumentManager.getInstance(project).commitDocument(containingFileEditor.document)

                        dialogWithEditor?.close(DialogWrapper.OK_EXIT_CODE)
                        if (brokenOff && !ApplicationManager.getApplication().isUnitTestMode) return

                        // file templates
                        val newDeclaration = PsiTreeUtil.findElementOfClassAtOffset(
                            jetFileToEdit,
                            declarationMarker.startOffset,
                            declaration::class.java,
                            false
                        ) ?: return

                        runWriteAction {
                            postprocessDeclaration(newDeclaration)

                            // file templates
                            if (newDeclaration is KtNamedFunction || newDeclaration is KtSecondaryConstructor) {
                                setupDeclarationBody(newDeclaration as KtFunction)
                            }

                            if (newDeclaration is KtProperty) {
                                newDeclaration.getter?.let { setupDeclarationBody(it) }

                                if (newDeclaration.getter == null
                                    && newDeclaration.initializer == null
                                    && callableInfo is PropertyInfo
                                    && callableInfo.withInitializer
                                    && !callableInfo.isLateinitPreferred
                                ) {
                                    newDeclaration.initializer = KtPsiFactory(newDeclaration).createExpression("TODO(\"initialize me\")")
                                }
                            }

                            val callElement = config.originalElement as? KtCallElement
                            if (callElement != null) {
                                setupCallTypeArguments(callElement, expression?.currentTypeParameters ?: Collections.emptyList())
                            }

                            CodeStyleManager.getInstance(project).reformat(newDeclaration)

                            // change short type names to fully qualified ones (to be shortened below)
                            setupTypeReferencesForShortening(newDeclaration, parameterTypeExpressions)
                            if (!transformToJavaMemberIfApplicable(newDeclaration)) {
                                elementsToShorten.add(newDeclaration)
                                setupEditor(newDeclaration)
                            }
                        }
                    } finally {
                        declarationMarker.dispose()
                        finished = true
                        onFinish()
                    }
                }

                override fun templateCancelled(template: Template?) {
                    finishTemplate(true)
                }

                override fun templateFinished(template: Template, brokenOff: Boolean) {
                    finishTemplate(brokenOff)
                }
            })
        }

        fun showDialogIfNeeded() {
            if (!ApplicationManager.getApplication().isUnitTestMode && dialogWithEditor != null && !finished) {
                dialogWithEditor.show()
            }
        }
    }
}

// TODO: Simplify and use formatter as much as possible
@Suppress("UNCHECKED_CAST")
internal fun <D : KtNamedDeclaration> placeDeclarationInContainer(
    declaration: D,
    container: PsiElement,
    anchor: PsiElement,
    fileToEdit: KtFile = container.containingFile as KtFile
): D {
    val psiFactory = KtPsiFactory(container)
    val newLine = psiFactory.createNewLine()

    fun calcNecessaryEmptyLines(decl: KtDeclaration, after: Boolean): Int {
        var lineBreaksPresent = 0
        var neighbor: PsiElement? = null

        siblingsLoop@
        for (sibling in decl.siblings(forward = after, withItself = false)) {
            when (sibling) {
                is PsiWhiteSpace -> lineBreaksPresent += (sibling.text ?: "").count { it == '\n' }
                else -> {
                    neighbor = sibling
                    break@siblingsLoop
                }
            }
        }

        val neighborType = neighbor?.node?.elementType
        val lineBreaksNeeded = when {
            neighborType == KtTokens.LBRACE || neighborType == KtTokens.RBRACE -> 1
            neighbor is KtDeclaration && (neighbor !is KtProperty || decl !is KtProperty) -> 2
            else -> 1
        }

        return max(lineBreaksNeeded - lineBreaksPresent, 0)
    }

    val actualContainer = (container as? KtClassOrObject)?.getOrCreateBody() ?: container

    fun addDeclarationToClassOrObject(
        classOrObject: KtClassOrObject,
        declaration: KtNamedDeclaration
    ): KtNamedDeclaration {
        val classBody = classOrObject.getOrCreateBody()
        return if (declaration is KtNamedFunction) {
            val neighbor = PsiTreeUtil.skipSiblingsBackward(
                classBody.rBrace ?: classBody.lastChild!!,
                PsiWhiteSpace::class.java
            )
            classBody.addAfter(declaration, neighbor) as KtNamedDeclaration
        } else classBody.addAfter(declaration, classBody.lBrace!!) as KtNamedDeclaration
    }


    fun addNextToOriginalElementContainer(addBefore: Boolean): D {
        val sibling = anchor.parentsWithSelf.first { it.parent == actualContainer }
        return if (addBefore) {
            actualContainer.addBefore(declaration, sibling)
        } else {
            actualContainer.addAfter(declaration, sibling)
        } as D
    }

    val declarationInPlace = when {
        declaration is KtPrimaryConstructor -> {
            (container as KtClass).createPrimaryConstructorIfAbsent().replaced(declaration) as D
        }

        declaration is KtProperty && container !is KtBlockExpression -> {
            val sibling = actualContainer.getChildOfType<KtProperty>() ?: when (actualContainer) {
                is KtClassBody -> actualContainer.declarations.firstOrNull() ?: actualContainer.rBrace
                is KtFile -> actualContainer.declarations.first()
                else -> null
            }
            sibling?.let { actualContainer.addBefore(declaration, it) as D } ?: fileToEdit.add(declaration) as D
        }

        actualContainer.isAncestor(anchor, true) -> {
            val insertToBlock = container is KtBlockExpression
            if (insertToBlock) {
                val parent = container.parent
                if (parent is KtFunctionLiteral) {
                    if (!parent.isMultiLine()) {
                        parent.addBefore(newLine, container)
                        parent.addAfter(newLine, container)
                    }
                }
            }
            addNextToOriginalElementContainer(insertToBlock || declaration is KtTypeAlias)
        }

        container is KtFile -> container.add(declaration) as D

        container is PsiClass -> {
            if (declaration is KtSecondaryConstructor) {
                val wrappingClass = psiFactory.createClass("class ${container.name} {\n}")
                addDeclarationToClassOrObject(wrappingClass, declaration)
                (fileToEdit.add(wrappingClass) as KtClass).declarations.first() as D
            } else {
                fileToEdit.add(declaration) as D
            }
        }

        container is KtClassOrObject -> {
            var sibling: PsiElement? = container.declarations.lastOrNull { it::class == declaration::class }
            if (sibling == null && declaration is KtProperty) {
                sibling = container.body?.lBrace
            }

            insertMember(null, container, declaration, sibling)
        }
        else -> throw AssertionError("Invalid containing element: ${container.text}")
    }

    when (declaration) {
        is KtEnumEntry -> {
            val prevEnumEntry = declarationInPlace.siblings(forward = false, withItself = false).firstIsInstanceOrNull<KtEnumEntry>()
            if (prevEnumEntry != null) {
                if ((prevEnumEntry.prevSibling as? PsiWhiteSpace)?.text?.contains('\n') == true) {
                    declarationInPlace.parent.addBefore(psiFactory.createNewLine(), declarationInPlace)
                }
                val comma = psiFactory.createComma()
                if (prevEnumEntry.allChildren.any { it.node.elementType == KtTokens.COMMA }) {
                    declarationInPlace.add(comma)
                } else {
                    prevEnumEntry.add(comma)
                }
                val semicolon = prevEnumEntry.allChildren.firstOrNull { it.node?.elementType == KtTokens.SEMICOLON }
                if (semicolon != null) {
                    (semicolon.prevSibling as? PsiWhiteSpace)?.text?.let {
                        declarationInPlace.add(psiFactory.createWhiteSpace(it))
                    }
                    declarationInPlace.add(psiFactory.createSemicolon())
                    semicolon.delete()
                }
            }
        }
        !is KtPrimaryConstructor -> {
            val parent = declarationInPlace.parent
            calcNecessaryEmptyLines(declarationInPlace, false).let {
                if (it > 0) parent.addBefore(psiFactory.createNewLine(it), declarationInPlace)
            }
            calcNecessaryEmptyLines(declarationInPlace, true).let {
                if (it > 0) parent.addAfter(psiFactory.createNewLine(it), declarationInPlace)
            }
        }
    }
    return declarationInPlace
}

fun KtNamedDeclaration.getReturnTypeReference() = getReturnTypeReferences().singleOrNull()

internal fun KtNamedDeclaration.getReturnTypeReferences(): List<KtTypeReference> {
    return when (this) {
        is KtCallableDeclaration -> listOfNotNull(typeReference)
        is KtClassOrObject -> superTypeListEntries.mapNotNull { it.typeReference }
        else -> throw AssertionError("Unexpected declaration kind: $text")
    }
}

fun CallableBuilderConfiguration.createBuilder(): CallableBuilder = CallableBuilder(this)
