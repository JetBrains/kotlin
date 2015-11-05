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
import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.*
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
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
import org.jetbrains.kotlin.idea.core.CollectingNameValidator
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.refactoring.*
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.ClassKind
import org.jetbrains.kotlin.idea.util.DialogWithEditor
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
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
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeImpl
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.resolve.scopes.utils.memberScopeAsImportingScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import java.util.*
import kotlin.properties.Delegates

private val TEMPLATE_FROM_USAGE_FUNCTION_BODY = "New Kotlin Function Body.kt"
private val TEMPLATE_FROM_USAGE_SECONDARY_CONSTRUCTOR_BODY = "New Kotlin Secondary Constructor Body.kt"
private val ATTRIBUTE_FUNCTION_NAME = "FUNCTION_NAME"

/**
 * Represents a single choice for a type (e.g. parameter type or return type).
 */
class TypeCandidate(val theType: KotlinType, scope: HierarchicalScope? = null) {
    public val typeParameters: Array<TypeParameterDescriptor>
    var renderedType: String? = null
        private set
    var renderedTypeParameters: List<RenderedTypeParameter>? = null
        private set

    fun render(typeParameterNameMap: Map<TypeParameterDescriptor, String>, fakeFunction: FunctionDescriptor?) {
        renderedType = theType.renderShort(typeParameterNameMap);
        renderedTypeParameters = typeParameters.map {
            RenderedTypeParameter(it, it.getContainingDeclaration() == fakeFunction, typeParameterNameMap[it]!!)
        }
    }

    init {
        val typeParametersInType = theType.getTypeParameters()
        if (scope == null) {
            typeParameters = typeParametersInType.toTypedArray()
            renderedType = theType.renderShort(Collections.emptyMap());
        }
        else {
            typeParameters = getTypeParameterNamesNotInScope(typeParametersInType, scope).toTypedArray();
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
        val currentFile: KtFile,
        val currentEditor: Editor?,
        val isExtension: Boolean = false,
        val enableSubstitutions: Boolean = true
)

interface CallablePlacement {
    class WithReceiver(val receiverTypeCandidate: TypeCandidate): CallablePlacement
    class NoReceiver(val containingElement: PsiElement): CallablePlacement
}

class CallableBuilder(val config: CallableBuilderConfiguration) {
    private var finished: Boolean = false

    val currentFileContext: BindingContext
    val currentFileModule: ModuleDescriptor

    val pseudocode: Pseudocode? by lazy { config.originalElement.getContainingPseudocode(currentFileContext) }

    private val typeCandidates = HashMap<TypeInfo, List<TypeCandidate>>()

    init {
        val result = config.currentFile.analyzeFullyAndGetResult()
        currentFileContext = result.bindingContext
        currentFileModule = result.moduleDescriptor
    }

    public var placement: CallablePlacement by Delegates.notNull()

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

            val newTypes = LinkedHashSet(types.map { EqWrapper(it) })
            for (substitution in substitutions) {
                // each substitution can be applied or not, so we offer all options
                val toAdd = newTypes.map { it._type.substitute(substitution, typeInfo.variance) }
                // substitution.byType are type arguments, but they cannot already occur in the type before substitution
                val toRemove = newTypes.filter { substitution.byType in it._type }

                newTypes.addAll(toAdd.map { EqWrapper(it) })
                newTypes.removeAll(toRemove)
            }

            if (newTypes.isEmpty()) {
                newTypes.add(EqWrapper(currentFileModule.builtIns.anyType))
            }

            newTypes.map { TypeCandidate(it._type, scope) }.reversed()
        }
    }

    private fun buildNext(iterator: Iterator<CallableInfo>) {
        if (iterator.hasNext()) {
            val context = Context(iterator.next())
            runWriteAction { context.buildAndRunTemplate { buildNext(iterator) } }
            ApplicationManager.getApplication().invokeLater { context.showDialogIfNeeded() }
        }
        else {
            ShortenReferences.DEFAULT.process(elementsToShorten)
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
        val receiverClassDescriptor: ClassDescriptor?
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
                            placement.receiverTypeCandidate.theType.getConstructor().getDeclarationDescriptor() as? ClassDescriptor
                    val classDeclaration = receiverClassDescriptor?.let { DescriptorToSourceUtils.getSourceFromDescriptor(it) }
                    containingElement = if (!config.isExtension && classDeclaration != null) classDeclaration else config.currentFile
                }
                else -> throw IllegalArgumentException("Unexpected placement: $placement")
            }
            val receiverType = receiverClassDescriptor?.getDefaultType()

            val project = config.currentFile.getProject()

            if (containingElement.getContainingFile() != config.currentFile) {
                NavigationUtil.activateFileWithPsiElement(containingElement)
            }

            if (containingElement is KtElement) {
                jetFileToEdit = containingElement.getContainingKtFile()
                if (jetFileToEdit != config.currentFile) {
                    containingFileEditor = FileEditorManager.getInstance(project).getSelectedTextEditor()!!
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
                with(containingFileEditor.getSettings()) {
                    setAdditionalColumnsCount(config.currentEditor!!.getSettings().getRightMargin(project))
                    setAdditionalLinesCount(5)
                }
                jetFileToEdit = PsiDocumentManager.getInstance(project).getPsiFile(containingFileEditor.getDocument()) as KtFile
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
                            assert (typeCandidates.size() == 1) { "Ambiguous type candidates for type parameter $it: $typeCandidates" }
                            typeCandidates.first().theType
                        }
                        .subtract(substitutionMap.keySet())
                fakeFunction = createFakeFunctionDescriptor(scope, typeArgumentsForFakeFunction.size())
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

            val projections = receiverClassDescriptor.getTypeConstructor().getParameters()
                    .map { TypeProjectionImpl(it.getDefaultType()) }
            val memberScope = receiverClassDescriptor.getMemberScope(projections)

            return LexicalScopeImpl(memberScope.memberScopeAsImportingScope(), receiverClassDescriptor, false, null,
                                    "Scope with type parameters for ${receiverClassDescriptor.getName()}") {
                receiverClassDescriptor.typeConstructor.parameters.forEach { addClassifierDescriptor(it) }
            }
        }

        private fun collectSubstitutionsForReceiverTypeParameters(
                receiverType: KotlinType?,
                result: MutableMap<KotlinType, KotlinType>
        ) {
            if (placement is CallablePlacement.NoReceiver) return

            val classTypeParameters = receiverType?.getArguments() ?: Collections.emptyList()
            val ownerTypeArguments = (placement as? CallablePlacement.WithReceiver)?.receiverTypeCandidate?.theType?.getArguments()
                                     ?: Collections.emptyList()
            assert(ownerTypeArguments.size() == classTypeParameters.size())
            ownerTypeArguments.zip(classTypeParameters).forEach { result[it.first.getType()] = it.second.getType() }
        }

        private fun collectSubstitutionsForCallableTypeParameters(
                fakeFunction: FunctionDescriptor,
                typeArguments: Set<KotlinType>,
                result: MutableMap<KotlinType, KotlinType>) {
            for ((typeArgument, typeParameter) in typeArguments zip fakeFunction.getTypeParameters()) {
                result[typeArgument] = typeParameter.getDefaultType()
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
                    return if (callableInfo.parameterInfos.isNotEmpty() || callableInfo.kind == CallableKind.FUNCTION) "($list)" else list
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
                        if (containingElement is KtClassOrObject
                            && containingElement.isAncestor(config.originalElement)
                            && callableInfo.kind != CallableKind.SECONDARY_CONSTRUCTOR)
                            "private "
                        else ""

                val declaration : KtNamedDeclaration = when (callableInfo.kind) {
                    CallableKind.FUNCTION, CallableKind.SECONDARY_CONSTRUCTOR -> {
                        val body = when {
                            containingElement is KtClass && containingElement.isInterface() && !config.isExtension -> ""
                            else -> "{}"
                        }
                        if (callableInfo is FunctionInfo) {
                            val operatorModifier = if (callableInfo.isOperator) "operator " else ""
                            psiFactory.createFunction("${modifiers}${operatorModifier}fun<> $header $body")
                        }
                        else {
                            psiFactory.createSecondaryConstructor("${modifiers}constructor$paramList $body")
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
                                    if (!(targetParent is KtClass && targetParent.isEnum())) throw AssertionError("Enum class expected: ${targetParent.getText()}")
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
                        val valVar = if ((callableInfo as PropertyInfo).writable) "var" else "val"
                        psiFactory.createProperty("$modifiers$valVar<> $header")
                    }
                }

                if (assignmentToReplace != null) {
                    (declaration as KtProperty).setInitializer(assignmentToReplace.getRight())
                    return assignmentToReplace.replace(declaration) as KtCallableDeclaration
                }

                val newLine = psiFactory.createNewLine()

                fun calcNecessaryEmptyLines(decl: KtDeclaration, after: Boolean): Int {
                    var lineBreaksPresent: Int = 0
                    var neighbor: PsiElement? = null

                    siblingsLoop@
                    for (sibling in decl.siblings(forward = after, withItself = false)) {
                        when (sibling) {
                            is PsiWhiteSpace -> lineBreaksPresent += (sibling.getText() ?: "").count { it == '\n' }
                            else -> {
                                neighbor = sibling
                                break@siblingsLoop
                            }
                        }
                    }

                    val neighborType = neighbor?.getNode()?.getElementType()
                    val lineBreaksNeeded = when {
                        neighborType == KtTokens.LBRACE, neighborType == KtTokens.RBRACE -> 1
                        neighbor is KtDeclaration && (neighbor !is KtProperty || decl !is KtProperty) -> 2
                        else -> 1
                    }

                    return Math.max(lineBreaksNeeded - lineBreaksPresent, 0)
                }

                fun addNextToOriginalElementContainer(addBefore: Boolean): KtNamedDeclaration {
                    val actualContainer = (containingElement as? KtClassOrObject)?.getBody() ?: containingElement
                    val sibling = config.originalElement.parentsWithSelf.first { it.getParent() == actualContainer }
                    return if (addBefore) {
                        actualContainer.addBefore(declaration, sibling)
                    }
                    else {
                        actualContainer.addAfter(declaration, sibling)
                    } as KtNamedDeclaration
                }

                val declarationInPlace = when {
                    containingElement.isAncestor(config.originalElement, true) -> {
                        val insertToBlock = containingElement is KtBlockExpression
                        if (insertToBlock) {
                            val parent = containingElement.getParent()
                            if (parent is KtFunctionLiteral) {
                                if (!parent.isMultiLine()) {
                                    parent.addBefore(newLine, containingElement)
                                    parent.addAfter(newLine, containingElement)
                                }
                            }
                        }
                        addNextToOriginalElementContainer(insertToBlock || declaration is KtProperty)
                    }

                    containingElement is KtFile -> containingElement.add(declaration) as KtNamedDeclaration

                    containingElement is PsiClass -> {
                        if (declaration is KtSecondaryConstructor) {
                            val wrappingClass = psiFactory.createClass("class ${containingElement.getName()} {\n}")
                            addDeclarationToClassOrObject(wrappingClass, declaration)
                            (jetFileToEdit.add(wrappingClass) as KtClass).getDeclarations().first() as KtNamedDeclaration
                        }
                        else {
                            jetFileToEdit.add(declaration) as KtNamedDeclaration
                        }
                    }

                    containingElement is KtClassOrObject -> {
                        addDeclarationToClassOrObject(containingElement, declaration)
                    }
                    else -> throw AssertionError("Invalid containing element: ${containingElement.getText()}")
                }

                val parent = declarationInPlace.getParent()
                calcNecessaryEmptyLines(declarationInPlace, false).let {
                    if (it > 0) parent.addBefore(psiFactory.createNewLine(it), declarationInPlace)
                }
                calcNecessaryEmptyLines(declarationInPlace, true).let {
                    if (it > 0) parent.addAfter(psiFactory.createNewLine(it), declarationInPlace)
                }

                return declarationInPlace
            }
        }

        private fun addDeclarationToClassOrObject(classOrObject: KtClassOrObject,
                                                  declaration: KtNamedDeclaration): KtNamedDeclaration {
            val classBody = classOrObject.getOrCreateBody()
            return if (declaration is KtNamedFunction) {
                val anchor = PsiTreeUtil.skipSiblingsBackward(
                        classBody.rBrace ?: classBody.getLastChild()!!,
                        javaClass<PsiWhiteSpace>()
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
            val typeParameterNames = allTypeParametersNotInScope.map { KotlinNameSuggester.suggestNameByName(it.getName().asString(), validator) }

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
                        returnTypeRef.getText()
                        ?: throw AssertionError("Expression for return type shouldn't be empty: declaration = ${declaration.getText()}")
                )
                if (returnType != null) {
                    // user selected a given type
                    replaceWithLongerName(returnTypeRef, returnType)
                    typeRefsToShorten.add(declaration.getReturnTypeReference()!!)
                }
            }

            val valueParameters = declaration.getValueParameters()
            val parameterIndicesToShorten = ArrayList<Int>()
            assert(valueParameters.size() == parameterTypeExpressions.size())
            for ((i, parameter) in valueParameters.asSequence().withIndex()) {
                val parameterTypeRef = parameter.getTypeReference()
                if (parameterTypeRef != null) {
                    val parameterType = parameterTypeExpressions[i].typeCandidates.getTypeByRenderedType(
                            parameterTypeRef.getText()
                            ?: throw AssertionError("Expression for parameter type shouldn't be empty: declaration = ${declaration.getText()}")
                    )
                    if (parameterType != null) {
                        replaceWithLongerName(parameterTypeRef, parameterType)
                        parameterIndicesToShorten.add(i)
                    }
                }
            }

            val expandedValueParameters = declaration.getValueParameters()
            parameterIndicesToShorten.asSequence()
                    .map { expandedValueParameters[it].getTypeReference() }
                    .filterNotNullTo(typeRefsToShorten)

            return typeRefsToShorten
        }

        private fun setupFunctionBody(func: KtFunction) {
            val oldBody = func.getBodyExpression() ?: return

            val templateName = when (func) {
                is KtSecondaryConstructor -> TEMPLATE_FROM_USAGE_SECONDARY_CONSTRUCTOR_BODY
                is KtNamedFunction -> TEMPLATE_FROM_USAGE_FUNCTION_BODY
                else -> throw AssertionError("Unexpected declaration: " + func.getElementTextWithContext())
            }
            val fileTemplate = FileTemplateManager.getInstance(func.getProject())!!.getCodeTemplate(templateName)
            val properties = Properties()
            properties.setProperty(FileTemplate.ATTRIBUTE_RETURN_TYPE, if (skipReturnType) "Unit" else func.getTypeReference()!!.getText())
            receiverClassDescriptor?.let {
                properties.setProperty(FileTemplate.ATTRIBUTE_CLASS_NAME, DescriptorUtils.getFqName(it).asString())
                properties.setProperty(FileTemplate.ATTRIBUTE_SIMPLE_CLASS_NAME, it.getName().asString())
            }
            if (callableInfo.name.isNotEmpty()) {
                properties.setProperty(ATTRIBUTE_FUNCTION_NAME, callableInfo.name)
            }

            val bodyText = try {
                fileTemplate!!.getText(properties)
            }
            catch (e: ProcessCanceledException) {
                throw e
            }
            catch (e: Exception) {
                // TODO: This is dangerous.
                // Is there any way to avoid catching all exceptions?
                throw IncorrectOperationException("Failed to parse file template", e)
            }

            oldBody.replace(KtPsiFactory(func).createBlock(bodyText))
        }

        private fun setupCallTypeArguments(callElement: KtCallElement, typeParameters: List<TypeParameterDescriptor>) {
            val oldTypeArgumentList = callElement.getTypeArgumentList() ?: return
            val renderedTypeArgs = typeParameters.map { typeParameter ->
                val type = substitutions.first { it.byType.getConstructor().getDeclarationDescriptor() == typeParameter }.forType
                IdeDescriptorRenderers.SOURCE_CODE.renderType(type)
            }
            if (renderedTypeArgs.isEmpty()) {
                oldTypeArgumentList.delete()
            }
            else {
                oldTypeArgumentList.replace(KtPsiFactory(callElement).createTypeArguments(renderedTypeArgs.joinToString(", ", "<", ">")))
                elementsToShorten.add(callElement.getTypeArgumentList()!!)
            }
        }

        private fun setupReturnTypeTemplate(builder: TemplateBuilder, declaration: KtNamedDeclaration): TypeExpression? {
            val candidates = typeCandidates[callableInfo.returnTypeInfo]!!
            if (candidates.isEmpty()) return null

            val elementToReplace: KtElement?
            val expression: TypeExpression
            when (declaration) {
                is KtCallableDeclaration -> {
                    elementToReplace = declaration.getTypeReference()
                    expression = TypeExpression.ForTypeReference(candidates)
                }
                is KtClassOrObject -> {
                    elementToReplace = declaration.getDelegationSpecifiers().firstOrNull()
                    expression = TypeExpression.ForDelegationSpecifier(candidates)
                }
                else -> throw AssertionError("Unexpected declaration kind: ${declaration.getText()}")
            }
            if (elementToReplace == null) return null

            if (candidates.size() == 1) {
                builder.replaceElement(elementToReplace, (expression.calculateResult(null) as TextResult).getText())
                return null
            }

            builder.replaceElement(elementToReplace, expression)
            return expression
        }

        private fun setupValVarTemplate(builder: TemplateBuilder, property: KtProperty) {
            if (!(callableInfo as PropertyInfo).writable) {
                builder.replaceElement(property.getValOrVarKeyword(), ValVarExpression)
            }
        }

        private fun setupTypeParameterListTemplate(
                builder: TemplateBuilderImpl,
                declaration: KtNamedDeclaration
        ): TypeParameterListExpression? {
            when (declaration) {
                is KtObjectDeclaration -> return null
                !is KtTypeParameterListOwner -> throw AssertionError("Unexpected declaration kind: ${declaration.getText()}")
            }

            val typeParameterList = (declaration as KtTypeParameterListOwner).getTypeParameterList() ?: return null

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
            assert(parameterList.size() == callableInfo.parameterInfos.size())

            val typeParameters = ArrayList<TypeExpression>()
            for ((parameter, jetParameter) in callableInfo.parameterInfos.zip(parameterList)) {
                val parameterTypeExpression = TypeExpression.ForTypeReference(typeCandidates[parameter.typeInfo]!!)
                val parameterTypeRef = jetParameter.getTypeReference()!!
                builder.replaceElement(parameterTypeRef, parameterTypeExpression)

                // add parameter name to the template
                val possibleNamesFromExpression = parameter.typeInfo.possibleNamesFromExpression
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
                val parameterNameIdentifier = jetParameter.getNameIdentifier()!!
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

                psiFactory.createPackageDirectiveIfNeeded(config.currentFile.getPackageFqName())?.let {
                    declaration.getContainingFile().addBefore(it, null)
                }

                val adjustedDeclaration = when (declaration) {
                    is KtNamedFunction, is KtProperty -> {
                        val klass = psiFactory.createClass("class Foo {}")
                        klass.getBody()!!.add(declaration)
                        (declaration.replace(klass) as KtClass).getBody()!!.getDeclarations().first()
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

            val project = declaration.getProject()

            val newJavaMember = convertToJava(targetClass) ?: return false

            val modifierList = newJavaMember.getModifierList()!!
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

            JavaCodeStyleManager.getInstance(project).shortenClassReferences(newJavaMember);

            val descriptor = OpenFileDescriptor(project, targetClass.getContainingFile().getVirtualFile())
            val targetEditor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true)!!

            when (newJavaMember) {
                is PsiMethod -> CreateFromUsageUtils.setupEditor(newJavaMember, targetEditor)
                is PsiField -> targetEditor.getCaretModel().moveToOffset(newJavaMember.endOffset - 1)
                is PsiClass -> {
                    val constructor = newJavaMember.getConstructors().firstOrNull()
                    val superStatement = constructor?.getBody()?.getStatements()?.firstOrNull() as? PsiExpressionStatement
                    val superCall = superStatement?.getExpression() as? PsiMethodCallExpression
                    if (superCall != null) {
                        val lParen = superCall.getArgumentList().getFirstChild()
                        targetEditor.getCaretModel().moveToOffset(lParen.endOffset)
                    }
                    else {
                        targetEditor.getCaretModel().moveToOffset(newJavaMember.startOffset)
                    }
                }
            }
            targetEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE)

            return true
        }

        private fun setupEditor(declaration: KtNamedDeclaration) {
            if (declaration is KtProperty && !declaration.hasInitializer() && containingElement is KtBlockExpression) {
                val defaultValueType = typeCandidates[callableInfo.returnTypeInfo]!!.firstOrNull()?.theType
                val defaultValue = defaultValueType?.let { CodeInsightUtils.defaultInitializer(it) } ?: "null"
                val initializer = declaration.setInitializer(KtPsiFactory(declaration).createExpression(defaultValue))!!
                val range = initializer.getTextRange()
                containingFileEditor.getSelectionModel().setSelection(range.getStartOffset(), range.getEndOffset())
                return
            }
            setupEditorSelection(containingFileEditor, declaration)
        }

        // build templates
        fun buildAndRunTemplate(onFinish: () -> Unit) {
            val declarationSkeleton = createDeclarationSkeleton()
            val project = declarationSkeleton.getProject()
            val declarationPointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(declarationSkeleton)

            // build templates
            PsiDocumentManager.getInstance(project).commitAllDocuments()
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(containingFileEditor.getDocument())

            val caretModel = containingFileEditor.getCaretModel()
            caretModel.moveToOffset(jetFileToEdit.getNode().getStartOffset())

            val declaration = declarationPointer.getElement()!!

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
            val variables = templateImpl.getVariables()!!
            if (variables.isNotEmpty()) {
                val typeParametersVar = if (expression != null) variables.remove(0) else null
                for (i in 0..(callableInfo.parameterInfos.size() - 1)) {
                    Collections.swap(variables, i * 2, i * 2 + 1)
                }
                typeParametersVar?.let { variables.add(it) }
            }

            // TODO: Disabled shortening names because it causes some tests fail. Refactor code to use automatic reference shortening
            templateImpl.setToShortenLongNames(false)

            // run the template
            TemplateManager.getInstance(project).startTemplate(containingFileEditor, templateImpl, object : TemplateEditingAdapter() {
                private fun finishTemplate(brokenOff: Boolean) {
                    try {
                        PsiDocumentManager.getInstance(project).commitDocument(containingFileEditor.getDocument())

                        dialogWithEditor?.close(DialogWrapper.OK_EXIT_CODE)
                        if (brokenOff && !ApplicationManager.getApplication().isUnitTestMode()) return

                        // file templates
                        val newDeclaration = if (templateImpl.getSegmentsCount() > 0) {
                            PsiTreeUtil.findElementOfClassAtOffset(jetFileToEdit, templateImpl.getSegmentOffset(0), declaration.javaClass, false)!!
                        }
                        else declarationPointer.getElement()!!

                        runWriteAction {
                            // file templates
                            if (newDeclaration is KtNamedFunction || newDeclaration is KtSecondaryConstructor) {
                                setupFunctionBody(newDeclaration as KtFunction)
                            }

                            val callElement = config.originalElement as? KtCallElement
                            if (callElement != null) {
                                setupCallTypeArguments(callElement, expression?.currentTypeParameters ?: Collections.emptyList())
                            }
                        }

                        // change short type names to fully qualified ones (to be shortened below)
                        val typeRefsToShorten = setupTypeReferencesForShortening(newDeclaration, parameterTypeExpressions)
                        if (!transformToJavaMemberIfApplicable(newDeclaration)) {
                            elementsToShorten.addAll(typeRefsToShorten)
                            setupEditor(newDeclaration)
                        }
                    }
                    finally {
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
            if (!ApplicationManager.getApplication().isUnitTestMode() && dialogWithEditor != null && !finished) {
                dialogWithEditor.show()
            }
        }
    }
}

internal fun KtNamedDeclaration.getReturnTypeReference(): KtTypeReference? {
    return when (this) {
        is KtCallableDeclaration -> getTypeReference()
        is KtClassOrObject -> getDelegationSpecifiers().firstOrNull()?.getTypeReference()
        else -> throw AssertionError("Unexpected declaration kind: ${getText()}")
    }
}

fun CallableBuilderConfiguration.createBuilder(): CallableBuilder = CallableBuilder(this)
