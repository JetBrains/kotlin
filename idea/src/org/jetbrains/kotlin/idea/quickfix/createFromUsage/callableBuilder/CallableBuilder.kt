/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import org.jetbrains.kotlin.cfg.pseudocode.getContainingPseudocode
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.MutablePackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.kotlin.idea.caches.resolve.getJavaClassDescriptor
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.ClassKind
import org.jetbrains.kotlin.idea.refactoring.*
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
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import java.lang.AssertionError
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.util.*

/**
 * Represents a single choice for a type (e.g. parameter type or return type).
 */
class TypeCandidate(val theType: KotlinType, scope: HierarchicalScope? = null) {
    val typeParameters: Array<TypeParameterDescriptor>
    var renderedType: String? = null
        private set
    var renderedTypeParameters: List<RenderedTypeParameter>? = null
        private set

    fun render(typeParameterNameMap: Map<TypeParameterDescriptor, String>, fakeFunction: FunctionDescriptor?) {
        renderedType = theType.renderShort(typeParameterNameMap)
        renderedTypeParameters = typeParameters.map {
            RenderedTypeParameter(it, it.containingDeclaration == fakeFunction, typeParameterNameMap[it]!!)
        }
    }

    init {
        val typeParametersInType = theType.getTypeParameters()
        if (scope == null) {
            typeParameters = typeParametersInType.toTypedArray()
            renderedType = theType.renderShort(Collections.emptyMap())
        }
        else {
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

fun List<TypeCandidate>.getTypeByRenderedType(renderedType: String): KotlinType? =
        firstOrNull { it.renderedType == renderedType }?.theType

class CallableBuilderConfiguration(
        val callableInfos: List<CallableInfo>,
        val originalElement: KtElement,
        val currentFile: KtFile = originalElement.getContainingKtFile(),
        val currentEditor: Editor? = null,
        val isExtension: Boolean = false,
        val enableSubstitutions: Boolean = true
)

sealed class CallablePlacement {
    class WithReceiver(val receiverTypeCandidate: TypeCandidate): CallablePlacement()
    class NoReceiver(val containingElement: PsiElement): CallablePlacement()
}

class CallableBuilder(val config: CallableBuilderConfiguration) {
    private var finished: Boolean = false

    val currentFileContext: BindingContext
    val currentFileModule: ModuleDescriptor

    init {
        val result = config.currentFile.analyzeFullyAndGetResult()
        currentFileContext = result.bindingContext
        currentFileModule = result.moduleDescriptor
    }

    val pseudocode: Pseudocode? by lazy { config.originalElement.getContainingPseudocode(currentFileContext) }

    private val typeCandidates = HashMap<TypeInfo, List<TypeCandidate>>()

    var placement: CallablePlacement? = null

    private val elementsToShorten = ArrayList<KtElement>()

    fun computeTypeCandidates(typeInfo: TypeInfo): List<TypeCandidate> =
            typeCandidates.getOrPut(typeInfo) { typeInfo.getPossibleTypes(this).map { TypeCandidate(it) } }

    private fun computeTypeCandidates(
            typeInfo: TypeInfo,
            substitutions: List<KotlinTypeSubstitution>,
            scope: HierarchicalScope): List<TypeCandidate> {
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
        }
        else {
            runWriteAction { ShortenReferences.DEFAULT.process(elementsToShorten) }
        }
    }

    fun build() {
        try {
            assert(config.currentEditor != null) { "Can't run build() without editor" }
            if (finished) throw IllegalStateException("Current builder has already finished")
            buildNext(config.callableInfos.iterator())
        }
        finally {
            finished = true
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
            when (placement) {
                is CallablePlacement.NoReceiver -> {
                    containingElement = placement.containingElement
                    receiverClassDescriptor = with (placement.containingElement) {
                        when (this) {
                            is KtClassOrObject -> currentFileContext[BindingContext.CLASS, this]
                            is PsiClass -> getJavaClassDescriptor()
                            else -> null
                        }
                    }
                }
                is CallablePlacement.WithReceiver -> {
                    receiverClassDescriptor =
                            placement.receiverTypeCandidate.theType.constructor.declarationDescriptor
                    val classDeclaration = receiverClassDescriptor?.let { DescriptorToSourceUtils.getSourceFromDescriptor(it) }
                    containingElement = if (!config.isExtension && classDeclaration != null) classDeclaration else config.currentFile
                }
                else -> throw IllegalArgumentException("Placement wan't initialized")
            }
            val receiverType = receiverClassDescriptor?.defaultType

            val project = config.currentFile.project

            if (containingElement.containingFile != config.currentFile) {
                NavigationUtil.activateFileWithPsiElement(containingElement)
            }

            if (containingElement is KtElement) {
                jetFileToEdit = containingElement.getContainingKtFile()
                if (jetFileToEdit != config.currentFile) {
                    containingFileEditor = FileEditorManager.getInstance(project).selectedTextEditor!!
                }
                else {
                    containingFileEditor = config.currentEditor!!
                }
                dialogWithEditor = null
            } else {
                val dialog = object: DialogWithEditor(project, "Create from usage", "") {
                    override fun doOKAction() {
                        project.executeWriteCommand("Premature end of template") {
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
                dialogWithEditor = dialog
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
                            assert (typeCandidates.size == 1) { "Ambiguous type candidates for type parameter $it: $typeCandidates" }
                            typeCandidates.first().theType
                        }
                        .subtract(substitutionMap.keys)
                fakeFunction = createFakeFunctionDescriptor(scope, typeArgumentsForFakeFunction.size)
                collectSubstitutionsForCallableTypeParameters(fakeFunction, typeArgumentsForFakeFunction, substitutionMap)
                mandatoryTypeParametersAsCandidates = receiverTypeCandidate.singletonOrEmptyList() + typeArgumentsForFakeFunction.map { TypeCandidate(substitutionMap[it]!!, scope) }
            }
            else {
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
                CallableKind.SECONDARY_CONSTRUCTOR -> true
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

            assert (receiverClassDescriptor is JavaClassDescriptor) { "Unexpected receiver class: $receiverClassDescriptor" }

            val projections = ((receiverClassDescriptor as JavaClassDescriptor).declaredTypeParameters)
                    .map { TypeProjectionImpl(it.defaultType) }
            val memberScope = receiverClassDescriptor.getMemberScope(projections)

            return LexicalScopeImpl(memberScope.memberScopeAsImportingScope(), receiverClassDescriptor, false, null,
                                    LexicalScopeKind.SYNTHETIC) {
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
                result: MutableMap<KotlinType, KotlinType>) {
            for ((typeArgument, typeParameter) in typeArguments.zip(fakeFunction.typeParameters)) {
                result[typeArgument] = typeParameter.defaultType
            }
        }

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
            val typeParameters = (0..typeParameterCount - 1).map {
                TypeParameterDescriptorImpl.createWithDefaultBound(
                        fakeFunction,
                        Annotations.EMPTY,
                        false,
                        Variance.INVARIANT,
                        Name.identifier(parameterNames[it]),
                        it
                )
            }

            return fakeFunction.initialize(null, null, typeParameters, Collections.emptyList(), null,
                                           null, Visibilities.INTERNAL)
        }

        private fun renderTypeCandidates(
                typeInfo: TypeInfo,
                typeParameterNameMap: Map<TypeParameterDescriptor, String>,
                fakeFunction: FunctionDescriptor?
        ) {
            typeCandidates[typeInfo]?.forEach { it.render(typeParameterNameMap, fakeFunction) }
        }

        private fun createDeclarationSkeleton(): KtNamedDeclaration {
            with (config) {
                val assignmentToReplace =
                        if (containingElement is KtBlockExpression && (callableInfo as? PropertyInfo)?.writable ?: false) {
                            originalElement as KtBinaryExpression
                        }
                        else null

                val ownerTypeString = if (isExtension) {
                    val renderedType = receiverTypeCandidate!!.renderedType!!
                    val isFunctionType = receiverTypeCandidate.theType.constructor.declarationDescriptor is FunctionClassDescriptor
                    if (isFunctionType) "($renderedType)." else "$renderedType."
                } else ""

                val classKind = (callableInfo as? PrimaryConstructorInfo)?.classInfo?.kind

                fun renderParamList(): String {
                    val prefix = if (classKind == ClassKind.ANNOTATION_CLASS) "val " else ""
                    val list = callableInfo.parameterInfos.indices.map { i -> "${prefix}p$i: Any" }.joinToString(", ")
                    return if (callableInfo.parameterInfos.isNotEmpty()
                               || callableInfo.kind == CallableKind.FUNCTION
                               || callableInfo.kind == CallableKind.SECONDARY_CONSTRUCTOR) "($list)" else list
                }

                val paramList = when (callableInfo.kind) {
                    CallableKind.FUNCTION, CallableKind.CLASS_WITH_PRIMARY_CONSTRUCTOR, CallableKind.SECONDARY_CONSTRUCTOR ->
                        renderParamList()
                    CallableKind.PROPERTY -> ""
                }
                val returnTypeString = if (skipReturnType || assignmentToReplace != null) "" else ": Any"
                val header = "$ownerTypeString${callableInfo.name.quoteIfNeeded()}$paramList$returnTypeString"

                val psiFactory = KtPsiFactory(currentFile)

                val modifiers =
                        if (callableInfo.isAbstract) {
                            if (containingElement is KtClass && containingElement.isInterface()) "" else "abstract "
                        }
                        else if (containingElement is KtClassOrObject
                                 && !(containingElement is KtClass && containingElement.isInterface())
                                 && containingElement.isAncestor(config.originalElement)
                                 && callableInfo.kind != CallableKind.SECONDARY_CONSTRUCTOR) "private "
                        else if (isExtension) "private "
                        else ""

                val declaration: KtNamedDeclaration = when (callableInfo.kind) {
                    CallableKind.FUNCTION, CallableKind.SECONDARY_CONSTRUCTOR -> {
                        val body = when {
                            callableInfo.kind == CallableKind.SECONDARY_CONSTRUCTOR -> ""
                            callableInfo.isAbstract -> ""
                            else -> "{}"
                        }
                        @Suppress("USELESS_CAST") // KT-10755
                        if (callableInfo is FunctionInfo) {
                            val operatorModifier = if (callableInfo.isOperator) "operator " else ""
                            val infixModifier = if (callableInfo.isInfix) "infix " else ""
                            psiFactory.createFunction("$modifiers$infixModifier${operatorModifier}fun<> $header $body") as KtNamedDeclaration
                        }
                        else {
                            psiFactory.createSecondaryConstructor("${modifiers}constructor$paramList $body") as KtNamedDeclaration
                        }
                    }
                    CallableKind.CLASS_WITH_PRIMARY_CONSTRUCTOR -> {
                        with((callableInfo as PrimaryConstructorInfo).classInfo) {
                            val classBody = when (kind) {
                                ClassKind.ANNOTATION_CLASS, ClassKind.ENUM_ENTRY -> ""
                                else -> "{\n\n}"
                            }
                            val safeName = name.quoteIfNeeded()
                            when (kind) {
                                ClassKind.ENUM_ENTRY -> {
                                    if (!(targetParent is KtClass && targetParent.isEnum())) throw AssertionError("Enum class expected: ${targetParent.text}")
                                    val hasParameters = targetParent.getPrimaryConstructorParameters().isNotEmpty()
                                    psiFactory.createEnumEntry("$safeName${if (hasParameters) "()" else " "}")
                                }
                                else -> {
                                    val openMod = if (open) "open " else ""
                                    val innerMod = if (inner) "inner " else ""
                                    val typeParamList = when (kind) {
                                        ClassKind.PLAIN_CLASS, ClassKind.INTERFACE -> "<>"
                                        else -> ""
                                    }
                                    psiFactory.createDeclaration<KtClassOrObject>(
                                            "$openMod$innerMod${kind.keyword} $safeName$typeParamList$paramList$returnTypeString $classBody"
                                    )
                                }
                            }
                        }
                    }
                    CallableKind.PROPERTY -> {
                        val isVar = (callableInfo as PropertyInfo).writable
                        val valVar = if (isVar) "var" else "val"
                        val accessors = if (isExtension) {
                            buildString {
                                append("\nget() {}")
                                if (isVar) {
                                    append("\nset() {}")
                                }
                            }
                        }
                        else ""
                        psiFactory.createProperty("$modifiers$valVar<> $header$accessors")
                    }
                }

                if (assignmentToReplace != null) {
                    (declaration as KtProperty).initializer = assignmentToReplace.right
                    return assignmentToReplace.replace(declaration) as KtCallableDeclaration
                }

                val newLine = psiFactory.createNewLine()

                fun calcNecessaryEmptyLines(decl: KtDeclaration, after: Boolean): Int {
                    var lineBreaksPresent: Int = 0
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

                    return Math.max(lineBreaksNeeded - lineBreaksPresent, 0)
                }

                val actualContainer = (containingElement as? KtClassOrObject)?.getOrCreateBody() ?: containingElement

                fun addNextToOriginalElementContainer(addBefore: Boolean): KtNamedDeclaration {
                    val sibling = config.originalElement.parentsWithSelf.first { it.parent == actualContainer }
                    return if (addBefore) {
                        actualContainer.addBefore(declaration, sibling)
                    }
                    else {
                        actualContainer.addAfter(declaration, sibling)
                    } as KtNamedDeclaration
                }

                val declarationInPlace = when {
                    actualContainer.isAncestor(config.originalElement, true) -> {
                        val insertToBlock = containingElement is KtBlockExpression
                        if (insertToBlock) {
                            val parent = containingElement.parent
                            if (parent is KtFunctionLiteral) {
                                if (!parent.isMultiLine()) {
                                    parent.addBefore(newLine, containingElement)
                                    parent.addAfter(newLine, containingElement)
                                }
                            }
                        }
                        addNextToOriginalElementContainer(insertToBlock || (declaration is KtProperty && actualContainer !is KtFile))
                    }

                    containingElement is KtFile -> containingElement.add(declaration) as KtNamedDeclaration

                    containingElement is PsiClass -> {
                        if (declaration is KtSecondaryConstructor) {
                            val wrappingClass = psiFactory.createClass("class ${containingElement.name} {\n}")
                            addDeclarationToClassOrObject(wrappingClass, declaration)
                            (jetFileToEdit.add(wrappingClass) as KtClass).declarations.first() as KtNamedDeclaration
                        }
                        else {
                            jetFileToEdit.add(declaration) as KtNamedDeclaration
                        }
                    }

                    containingElement is KtClassOrObject -> {
                        insertMember(null, containingElement, declaration, containingElement.declarations.lastOrNull())
                    }
                    else -> throw AssertionError("Invalid containing element: ${containingElement.text}")
                }

                val parent = declarationInPlace.parent
                calcNecessaryEmptyLines(declarationInPlace, false).let {
                    if (it > 0) parent.addBefore(psiFactory.createNewLine(it), declarationInPlace)
                }
                calcNecessaryEmptyLines(declarationInPlace, true).let {
                    if (it > 0) parent.addAfter(psiFactory.createNewLine(it), declarationInPlace)
                }

                if (declarationInPlace is KtSecondaryConstructor) {
                    val containingClass = declarationInPlace.containingClassOrObject!!
                    if (containingClass.getPrimaryConstructorParameters().isNotEmpty()) {
                        declarationInPlace.replaceImplicitDelegationCallWithExplicit(true)
                    }
                    else if ((receiverClassDescriptor as ClassDescriptor).getSuperClassOrAny().constructors.all { it.valueParameters.isNotEmpty() }) {
                        declarationInPlace.replaceImplicitDelegationCallWithExplicit(false)
                    }
                }

                return declarationInPlace
            }
        }

        private fun addDeclarationToClassOrObject(classOrObject: KtClassOrObject,
                                                  declaration: KtNamedDeclaration): KtNamedDeclaration {
            val classBody = classOrObject.getOrCreateBody()
            return if (declaration is KtNamedFunction) {
                val anchor = PsiTreeUtil.skipSiblingsBackward(
                        classBody.rBrace ?: classBody.lastChild!!,
                        PsiWhiteSpace::class.java
                )
                classBody.addAfter(declaration, anchor) as KtNamedDeclaration
            }
            else classBody.addAfter(declaration, classBody.lBrace!!) as KtNamedDeclaration
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
            val typeParameterNames = allTypeParametersNotInScope.map { KotlinNameSuggester.suggestNameByName(it.name.asString(), validator) }

            return allTypeParametersNotInScope.zip(typeParameterNames).toMap()
        }

        private fun setupTypeReferencesForShortening(declaration: KtNamedDeclaration,
                                                     parameterTypeExpressions: List<TypeExpression>): List<KtElement> {
            val typeRefsToShorten = ArrayList<KtElement>()

            if (config.isExtension) {
                val receiverTypeText = receiverTypeCandidate!!.theType.renderLong(typeParameterNameMap)
                val replacingTypeRef = KtPsiFactory(declaration).createType(receiverTypeText)
                val newTypeRef = (declaration as KtCallableDeclaration).setReceiverTypeReference(replacingTypeRef)!!
                typeRefsToShorten.add(newTypeRef)
            }

            val returnTypeRef = declaration.getReturnTypeReference()
            if (returnTypeRef != null) {
                val returnType = typeCandidates[callableInfo.returnTypeInfo]!!.getTypeByRenderedType(
                        returnTypeRef.text
                        ?: throw AssertionError("Expression for return type shouldn't be empty: declaration = ${declaration.text}")
                )
                if (returnType != null) {
                    // user selected a given type
                    replaceWithLongerName(returnTypeRef, returnType)
                    typeRefsToShorten.add(declaration.getReturnTypeReference()!!)
                }
            }

            val valueParameters = declaration.getValueParameters()
            val parameterIndicesToShorten = ArrayList<Int>()
            assert(valueParameters.size == parameterTypeExpressions.size)
            for ((i, parameter) in valueParameters.asSequence().withIndex()) {
                val parameterTypeRef = parameter.typeReference
                if (parameterTypeRef != null) {
                    val parameterType = parameterTypeExpressions[i].typeCandidates.getTypeByRenderedType(
                            parameterTypeRef.text
                            ?: throw AssertionError("Expression for parameter type shouldn't be empty: declaration = ${declaration.text}")
                    )
                    if (parameterType != null) {
                        replaceWithLongerName(parameterTypeRef, parameterType)
                        parameterIndicesToShorten.add(i)
                    }
                }
            }

            val expandedValueParameters = declaration.getValueParameters()
            parameterIndicesToShorten.mapNotNullTo(typeRefsToShorten) { expandedValueParameters[it].typeReference }

            return typeRefsToShorten
        }

        private fun setupDeclarationBody(func: KtDeclarationWithBody) {
            val oldBody = func.bodyExpression ?: return
            val templateKind = when (func) {
                is KtSecondaryConstructor -> TemplateKind.SECONDARY_CONSTRUCTOR
                is KtNamedFunction, is KtPropertyAccessor -> TemplateKind.FUNCTION
                else -> throw AssertionError("Unexpected declaration: " + func.getElementTextWithContext())
            }
            val bodyText = getFunctionBodyTextFromTemplate(
                    func.project,
                    templateKind,
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
            }
            else {
                oldTypeArgumentList.replace(KtPsiFactory(callElement).createTypeArguments(renderedTypeArgs.joinToString(", ", "<", ">")))
                elementsToShorten.add(callElement.typeArgumentList!!)
            }
        }

        private fun setupReturnTypeTemplate(builder: TemplateBuilder, declaration: KtNamedDeclaration): TypeExpression? {
            val candidates = typeCandidates[callableInfo.returnTypeInfo]!!
            if (candidates.isEmpty()) return null

            val elementToReplace: KtElement?
            val expression: TypeExpression
            when (declaration) {
                is KtCallableDeclaration -> {
                    elementToReplace = declaration.typeReference
                    expression = TypeExpression.ForTypeReference(candidates)
                }
                is KtClassOrObject -> {
                    elementToReplace = declaration.getSuperTypeListEntries().firstOrNull()
                    expression = TypeExpression.ForDelegationSpecifier(candidates)
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
                    .forEach { typeParameterMap[it.renderedType!!] = it.renderedTypeParameters!! }

            if (declaration.getReturnTypeReference() != null) {
                typeCandidates[callableInfo.returnTypeInfo]!!.forEach {
                    typeParameterMap[it.renderedType!!] = it.renderedTypeParameters!!
                }
            }

            val expression = TypeParameterListExpression(
                    mandatoryTypeParameters,
                    typeParameterMap,
                    callableInfo.kind != CallableKind.CLASS_WITH_PRIMARY_CONSTRUCTOR
            )
            builder.replaceElement(typeParameterList, expression, false)
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
                val preferredName = parameter.preferredName
                val possibleNames = if (preferredName != null) {
                    arrayOf(preferredName, *possibleNamesFromExpression)
                }
                else {
                    possibleNamesFromExpression
                }

                // figure out suggested names for each type option
                val parameterTypeToNamesMap = HashMap<String, Array<String>>()
                typeCandidates[parameter.typeInfo]!!.forEach { typeCandidate ->
                    val suggestedNames = KotlinNameSuggester.suggestNamesByType(typeCandidate.theType, { true })
                    parameterTypeToNamesMap[typeCandidate.renderedType!!] = suggestedNames.toTypedArray()
                }

                // add expression to builder
                val parameterNameExpression = ParameterNameExpression(possibleNames, parameterTypeToNamesMap)
                val parameterNameIdentifier = jetParameter.nameIdentifier!!
                builder.replaceElement(parameterNameIdentifier, parameterNameExpression)

                typeParameters.add(parameterTypeExpression)
            }
            return typeParameters
        }

        private fun replaceWithLongerName(typeRef: KtTypeReference, theType: KotlinType) {
            val fullyQualifiedReceiverTypeRef = KtPsiFactory(typeRef).createType(theType.renderLong(typeParameterNameMap))
            typeRef.replace(fullyQualifiedReceiverTypeRef)
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
                        klass.getBody()!!.add(declaration)
                        (declaration.replace(klass) as KtClass).getBody()!!.declarations.first()
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
                is PrimaryConstructorInfo -> with(callableInfo.classInfo) {
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
                    }
                    else {
                        targetEditor.caretModel.moveToOffset(newJavaMember.startOffset)
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

            val declaration = declarationPointer.element!!

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

            // the template built by TemplateBuilderImpl is ordered by element position, but we want types to be first, so hack it
            val templateImpl = builder.buildInlineTemplate() as TemplateImpl
            val variables = templateImpl.variables!!
            if (variables.isNotEmpty()) {
                val typeParametersVar = if (expression != null) variables.removeAt(0) else null
                for (i in 0..(callableInfo.parameterInfos.size - 1)) {
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
                        val newDeclaration = PsiTreeUtil.findElementOfClassAtOffset(jetFileToEdit,
                                                                                    declarationMarker.startOffset,
                                                                                    declaration.javaClass,
                                                                                    false) ?: return

                        runWriteAction {
                            // file templates
                            if (newDeclaration is KtNamedFunction || newDeclaration is KtSecondaryConstructor) {
                                setupDeclarationBody(newDeclaration as KtFunction)
                            }

                            if (newDeclaration is KtProperty) {
                                newDeclaration.getter?.let { setupDeclarationBody(it) }
                            }

                            val callElement = config.originalElement as? KtCallElement
                            if (callElement != null) {
                                setupCallTypeArguments(callElement, expression?.currentTypeParameters ?: Collections.emptyList())
                            }

                            CodeStyleManager.getInstance(project).reformat(newDeclaration)

                            // change short type names to fully qualified ones (to be shortened below)
                            val typeRefsToShorten = setupTypeReferencesForShortening(newDeclaration, parameterTypeExpressions)
                            if (!transformToJavaMemberIfApplicable(newDeclaration)) {
                                elementsToShorten.addAll(typeRefsToShorten)
                                setupEditor(newDeclaration)
                            }
                        }
                    }
                    finally {
                        declarationMarker.dispose()
                        finished = true
                        onFinish()
                    }
                }

                override fun templateCancelled(template: Template?) {
                    finishTemplate(true)
                }

                override fun templateFinished(template: Template?, brokenOff: Boolean) {
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

internal fun KtNamedDeclaration.getReturnTypeReference(): KtTypeReference? {
    return when (this) {
        is KtCallableDeclaration -> typeReference
        is KtClassOrObject -> getSuperTypeListEntries().firstOrNull()?.typeReference
        else -> throw AssertionError("Unexpected declaration kind: $text")
    }
}

fun CallableBuilderConfiguration.createBuilder(): CallableBuilder = CallableBuilder(this)
