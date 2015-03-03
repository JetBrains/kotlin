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

import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.codeInsight.template.*
import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import kotlin.properties.Delegates
import java.util.LinkedHashSet
import java.util.Collections
import java.util.HashMap
import java.util.ArrayList
import java.util.Properties
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.types.checker.JetTypeChecker
import com.intellij.psi.SmartPointerManager
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.descriptors.impl.MutablePackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import java.util.LinkedHashMap
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.ClassKind
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import org.jetbrains.kotlin.psi.psiUtil.siblings
import com.intellij.openapi.editor.ScrollType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import com.intellij.psi.PsiClass
import com.intellij.codeInsight.daemon.impl.quickfix.CreateFromUsageUtils
import com.intellij.psi.PsiMethod
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import org.jetbrains.kotlin.idea.caches.resolve.getJavaClassDescriptor
import org.jetbrains.kotlin.resolve.scopes.ChainedScope
import org.jetbrains.kotlin.resolve.scopes.WritableScopeImpl
import org.jetbrains.kotlin.resolve.scopes.RedeclarationHandler
import org.jetbrains.kotlin.resolve.scopes.WritableScope
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiExpressionStatement
import org.jetbrains.kotlin.idea.refactoring.*
import org.jetbrains.kotlin.idea.util.*
import com.intellij.openapi.ui.*
import com.intellij.codeInsight.template.impl.*
import org.jetbrains.kotlin.idea.util.application.*

private val TYPE_PARAMETER_LIST_VARIABLE_NAME = "typeParameterList"
private val TEMPLATE_FROM_USAGE_FUNCTION_BODY = "New Kotlin Function Body.kt"
private val ATTRIBUTE_FUNCTION_NAME = "FUNCTION_NAME"

/**
 * Represents a single choice for a type (e.g. parameter type or return type).
 */
class TypeCandidate(val theType: JetType, scope: JetScope? = null) {
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

    {
        val typeParametersInType = theType.getTypeParameters()
        if (scope == null) {
            typeParameters = typeParametersInType.copyToArray()
            renderedType = theType.renderShort(Collections.emptyMap());
        }
        else {
            typeParameters = getTypeParameterNamesNotInScope(typeParametersInType, scope).copyToArray();
        }
    }

    override fun toString() = theType.toString()
}

data class RenderedTypeParameter(
        val typeParameter: TypeParameterDescriptor,
        val fake: Boolean,
        val text: String
)

fun List<TypeCandidate>.getTypeByRenderedType(renderedType: String): JetType? =
        firstOrNull { it.renderedType == renderedType }?.theType

class CallableBuilderConfiguration(
        val callableInfos: List<CallableInfo>,
        val originalElement: JetElement,
        val currentFile: JetFile,
        val currentEditor: Editor?,
        val isExtension: Boolean = false,
        val enableSubstitutions: Boolean = true
)

trait CallablePlacement {
    class WithReceiver(val receiverTypeCandidate: TypeCandidate): CallablePlacement
    class NoReceiver(val containingElement: PsiElement): CallablePlacement
}

class CallableBuilder(val config: CallableBuilderConfiguration) {
    private var finished: Boolean = false

    val currentFileContext: BindingContext
    val currentFileModule: ModuleDescriptor

    private val typeCandidates = HashMap<TypeInfo, List<TypeCandidate>>();

    {
        val result = config.currentFile.analyzeFullyAndGetResult()
        currentFileContext = result.bindingContext
        currentFileModule = result.moduleDescriptor
    }

    public var placement: CallablePlacement by Delegates.notNull()

    private val elementsToShorten = ArrayList<JetElement>()

    fun computeTypeCandidates(typeInfo: TypeInfo): List<TypeCandidate> =
            typeCandidates.getOrPut(typeInfo) { typeInfo.getPossibleTypes(this).map { TypeCandidate(it) } }

    fun computeTypeCandidates(
            typeInfo: TypeInfo,
            substitutions: List<JetTypeSubstitution>,
            scope: JetScope): List<TypeCandidate> {
        if (!typeInfo.substitutionsAllowed) return computeTypeCandidates(typeInfo)
        return typeCandidates.getOrPut(typeInfo) {
            val types = typeInfo.getPossibleTypes(this).reverse()

            // We have to use semantic equality here
            [data] class EqWrapper(val _type: JetType) {
                override fun equals(other: Any?) = this === other
                                                   || other is EqWrapper && JetTypeChecker.DEFAULT.equalTypes(_type, other._type)
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

            if (newTypes.empty) {
                newTypes.add(EqWrapper(KotlinBuiltIns.getInstance().getAnyType()))
            }

            newTypes.map { TypeCandidate(it._type, scope) }.reverse()
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
            assert (config.currentEditor != null, "Can't run build() without editor")
            if (finished) throw IllegalStateException("Current builder has already finished")
            buildNext(config.callableInfos.iterator())
        }
        finally {
            finished = true
        }
    }

    private inner class Context(val callableInfo: CallableInfo) {
        val skipReturnType: Boolean
        val jetFileToEdit: JetFile
        val containingFileEditor: Editor
        val containingElement: PsiElement
        val dialogWithEditor: DialogWithEditor?
        val receiverClassDescriptor: ClassDescriptor?
        val typeParameterNameMap: Map<TypeParameterDescriptor, String>
        val receiverTypeCandidate: TypeCandidate?
        val mandatoryTypeParametersAsCandidates: List<TypeCandidate>
        val substitutions: List<JetTypeSubstitution>
        var released: Boolean = false

        {
            // gather relevant information

            val placement = placement
            when {
                placement is CallablePlacement.NoReceiver -> {
                    containingElement = placement.containingElement
                    receiverClassDescriptor = with (placement.containingElement) {
                        when (this) {
                            is JetClassOrObject -> currentFileContext[BindingContext.CLASS, this]
                            is PsiClass -> getJavaClassDescriptor()
                            else -> null
                        }
                    }
                }
                placement is CallablePlacement.WithReceiver -> {
                    receiverClassDescriptor =
                            placement.receiverTypeCandidate.theType.getConstructor().getDeclarationDescriptor() as? ClassDescriptor
                    val classDeclaration = receiverClassDescriptor?.let { DescriptorToSourceUtils.classDescriptorToDeclaration(it) }
                    containingElement = if (!config.isExtension && classDeclaration != null) classDeclaration else config.currentFile
                }
                else -> throw IllegalArgumentException("Unexpected placement: $placement")
            }
            val receiverType = receiverClassDescriptor?.getDefaultType()

            val project = config.currentFile.getProject()

            if (containingElement.getContainingFile() != config.currentFile) {
                NavigationUtil.activateFileWithPsiElement(containingElement)
            }

            if (containingElement is JetElement) {
                jetFileToEdit = containingElement.getContainingJetFile()
                if (jetFileToEdit != config.currentFile) {
                    containingFileEditor = FileEditorManager.getInstance(project).getSelectedTextEditor()
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
                jetFileToEdit = PsiDocumentManager.getInstance(project).getPsiFile(containingFileEditor.getDocument()) as JetFile
                jetFileToEdit.analysisContext = config.currentFile
                dialogWithEditor = dialog
            }

            val scope = getDeclarationScope()

            receiverTypeCandidate = receiverType?.let { TypeCandidate(it, scope) }

            val fakeFunction: FunctionDescriptor?
            // figure out type substitutions for type parameters
            val substitutionMap = LinkedHashMap<JetType, JetType>()
            if (config.enableSubstitutions) {
                collectSubstitutionsForReceiverTypeParameters(receiverType, substitutionMap)
                val typeArgumentsForFakeFunction = callableInfo.typeParameterInfos
                        .map {
                            val typeCandidates = computeTypeCandidates(it)
                            assert (typeCandidates.size == 1, "Ambiguous type candidates for type parameter $it: $typeCandidates")
                            typeCandidates.first().theType
                        }
                        .subtract(substitutionMap.keySet())
                fakeFunction = createFakeFunctionDescriptor(scope, typeArgumentsForFakeFunction.size)
                collectSubstitutionsForCallableTypeParameters(fakeFunction!!, typeArgumentsForFakeFunction, substitutionMap)
                mandatoryTypeParametersAsCandidates = receiverTypeCandidate.singletonOrEmptyList() + typeArgumentsForFakeFunction.map { TypeCandidate(substitutionMap[it], scope) }
            }
            else {
                fakeFunction = null
                mandatoryTypeParametersAsCandidates = Collections.emptyList()
            }
            substitutions = substitutionMap.map { JetTypeSubstitution(it.key, it.value) }

            callableInfo.parameterInfos.forEach {
                computeTypeCandidates(it.typeInfo, substitutions, scope)
            }

            val returnTypeCandidate = computeTypeCandidates(callableInfo.returnTypeInfo, substitutions, scope).singleOrNull()
            skipReturnType = when (callableInfo.kind) {
                CallableKind.FUNCTION ->
                    returnTypeCandidate?.theType?.isUnit() ?: false
                CallableKind.CONSTRUCTOR ->
                    callableInfo.returnTypeInfo == TypeInfo.Empty || returnTypeCandidate?.theType?.isAny() ?: false
                CallableKind.PROPERTY -> false
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

        private fun getDeclarationScope(): JetScope {
            if (config.isExtension || receiverClassDescriptor == null) {
                return currentFileModule.getPackage(config.currentFile.getPackageFqName())!!.getMemberScope()
            }

            if (receiverClassDescriptor is ClassDescriptorWithResolutionScopes) {
                return receiverClassDescriptor.getScopeForMemberDeclarationResolution()
            }

            assert (receiverClassDescriptor is JavaClassDescriptor, "Unexpected receiver class: ${receiverClassDescriptor}")

            val typeParamScope = with(
                    WritableScopeImpl(
                            JetScope.Empty,
                            receiverClassDescriptor,
                            RedeclarationHandler.DO_NOTHING,
                            "Scope with type parameters for ${receiverClassDescriptor.getName()}"
                    )
            ) {
                receiverClassDescriptor.getTypeConstructor().getParameters().forEach { addClassifierDescriptor(it) }
                changeLockLevel(WritableScope.LockLevel.READING)
            }

            val projections = receiverClassDescriptor.getTypeConstructor().getParameters()
                    .map { TypeProjectionImpl(it.getDefaultType()) }
            val memberScope = receiverClassDescriptor.getMemberScope(projections)

            return ChainedScope(
                    receiverClassDescriptor,
                    "Classifier resolution scope: ${receiverClassDescriptor.getName()}",
                    typeParamScope,
                    memberScope
            )
        }

        private fun collectSubstitutionsForReceiverTypeParameters(
                receiverType: JetType?,
                result: MutableMap<JetType, JetType>
        ) {
            if (placement is CallablePlacement.NoReceiver) return

            val classTypeParameters = receiverType?.getArguments() ?: Collections.emptyList()
            val ownerTypeArguments = (placement as? CallablePlacement.WithReceiver)?.receiverTypeCandidate?.theType?.getArguments()
                                     ?: Collections.emptyList()
            assert(ownerTypeArguments.size == classTypeParameters.size)
            ownerTypeArguments.zip(classTypeParameters).forEach { result[it.first.getType()] = it.second.getType() }
        }

        private fun collectSubstitutionsForCallableTypeParameters(
                fakeFunction: FunctionDescriptor,
                typeArguments: Set<JetType>,
                result: MutableMap<JetType, JetType>) {
            for ((typeArgument, typeParameter) in typeArguments zip fakeFunction.getTypeParameters()) {
                result[typeArgument] = typeParameter.getDefaultType()
            }
        }

        private fun createFakeFunctionDescriptor(scope: JetScope, typeParameterCount: Int): FunctionDescriptor {
            val fakeFunction = SimpleFunctionDescriptorImpl.create(
                    MutablePackageFragmentDescriptor(currentFileModule, FqName("fake")),
                    Annotations.EMPTY,
                    Name.identifier("fake"),
                    CallableMemberDescriptor.Kind.SYNTHESIZED,
                    SourceElement.NO_SOURCE
            )

            val validator = CollectingValidator { scope.getClassifier(Name.identifier(it)) == null }
            val parameterNames = JetNameSuggester.suggestNamesForTypeParameters(typeParameterCount, validator)
            val typeParameters = typeParameterCount.indices.map {
                TypeParameterDescriptorImpl.createWithDefaultBound(
                        fakeFunction,
                        Annotations.EMPTY,
                        false,
                        Variance.INVARIANT,
                        Name.identifier(parameterNames[it]),
                        it
                )
            }

            return fakeFunction.initialize(null, null, typeParameters, Collections.emptyList(), null, null, Visibilities.INTERNAL)
        }

        private fun renderTypeCandidates(
                typeInfo: TypeInfo,
                typeParameterNameMap: Map<TypeParameterDescriptor, String>,
                fakeFunction: FunctionDescriptor?
        ) {
            typeCandidates[typeInfo]?.forEach { it.render(typeParameterNameMap, fakeFunction) }
        }

        private fun createDeclarationSkeleton(): JetNamedDeclaration {
            with (config) {
                val assignmentToReplace =
                        if (containingElement is JetBlockExpression && (callableInfo as? PropertyInfo)?.writable ?: false) {
                            originalElement as JetBinaryExpression
                        }
                        else null

                val ownerTypeString = if (isExtension) "${receiverTypeCandidate!!.renderedType!!}." else ""

                val classKind = (callableInfo as? ConstructorInfo)?.classInfo?.kind

                fun renderParamList(): String {
                    val prefix = if (classKind == ClassKind.ANNOTATION_CLASS) "val " else ""
                    val list = callableInfo.parameterInfos.indices.map { i -> "${prefix}p$i: Any" }.joinToString(", ")
                    return if (callableInfo.parameterInfos.isNotEmpty() || callableInfo.kind == CallableKind.FUNCTION) "($list)" else list
                }

                val paramList = when (callableInfo.kind) {
                    CallableKind.FUNCTION, CallableKind.CONSTRUCTOR -> renderParamList()
                    CallableKind.PROPERTY -> ""
                }
                val returnTypeString = if (skipReturnType || assignmentToReplace != null) "" else ": Any"
                val header = "$ownerTypeString${callableInfo.name}$paramList$returnTypeString"

                val psiFactory = JetPsiFactory(currentFile)

                val modifiers =
                        if (containingElement is JetClassOrObject && containingElement.isAncestor(config.originalElement))
                            "private "
                        else ""

                val declaration : JetNamedDeclaration = when (callableInfo.kind) {
                    CallableKind.FUNCTION -> {
                        val body = when {
                            containingElement is JetClass && containingElement.isTrait() && !config.isExtension -> ""
                            else -> "{}"
                        }
                        psiFactory.createFunction("${modifiers}fun<> $header $body")
                    }
                    CallableKind.CONSTRUCTOR -> {
                        with((callableInfo as ConstructorInfo).classInfo) {
                            val classBody = when (kind) {
                                ClassKind.ANNOTATION_CLASS, ClassKind.ENUM_ENTRY -> ""
                                else -> "{\n\n}"
                            }
                            when (kind) {
                                ClassKind.ENUM_ENTRY -> {
                                    if (!(targetParent is JetClass && targetParent.isEnum())) throw AssertionError("Enum class expected: ${targetParent.getText()}")
                                    val hasParameters = targetParent.getPrimaryConstructorParameters().isNotEmpty()
                                    psiFactory.createEnumEntry("$name ${if (hasParameters) ": ${targetParent.getName()}()" else ""}")
                                }
                                else -> {
                                    val openMod = if (open) "open " else ""
                                    val innerMod = if (inner) "inner " else ""
                                    val typeParamList = when (kind) {
                                        ClassKind.PLAIN_CLASS, ClassKind.TRAIT -> "<>"
                                        else -> ""
                                    }
                                    psiFactory.createDeclaration<JetClassOrObject>(
                                            "$openMod$innerMod${kind.keyword} $name$typeParamList$paramList$returnTypeString $classBody"
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
                    (declaration as JetProperty).setInitializer(assignmentToReplace.getRight())
                    return assignmentToReplace.replace(declaration) as JetCallableDeclaration
                }

                val newLine = psiFactory.createNewLine()

                fun calcNecessaryEmptyLines(decl: JetDeclaration, after: Boolean): Int {
                    var lineBreaksPresent: Int = 0
                    var neighbor: PsiElement? = null
                    for (sibling in decl.siblings(forward = after, withItself = false)) {
                        when (sibling) {
                            is PsiWhiteSpace -> lineBreaksPresent += (sibling.getText() ?: "").count { it == '\n' }
                            else -> {
                                neighbor = sibling
                                break
                            }
                        }
                    }

                    val neighborType = neighbor?.getNode()?.getElementType()
                    val lineBreaksNeeded = when {
                        neighborType == JetTokens.LBRACE, neighborType == JetTokens.RBRACE -> 1
                        neighbor is JetDeclaration && (neighbor !is JetProperty || decl !is JetProperty) -> 2
                        else -> 1
                    }

                    return Math.max(lineBreaksNeeded - lineBreaksPresent, 0)
                }

                val declarationInPlace = when (containingElement) {
                    is JetFile -> containingElement.add(declaration) as JetNamedDeclaration

                    is PsiClass -> jetFileToEdit.add(declaration) as JetNamedDeclaration

                    is JetClassOrObject -> {
                        var classBody = containingElement.getBody()
                        if (classBody == null) {
                            classBody = containingElement.add(psiFactory.createEmptyClassBody()) as JetClassBody
                            containingElement.addBefore(psiFactory.createWhiteSpace(), classBody)
                        }

                        if (declaration is JetNamedFunction) {
                            val anchor = PsiTreeUtil.skipSiblingsBackward(
                                    classBody!!.getRBrace() ?: classBody!!.getLastChild()!!,
                                    javaClass<PsiWhiteSpace>()
                            )
                            classBody.addAfter(declaration, anchor) as JetNamedDeclaration
                        }
                        else classBody.addAfter(declaration, classBody!!.getLBrace()!!) as JetNamedDeclaration
                    }

                    is JetBlockExpression -> {
                        val parent = containingElement.getParent()
                        if (parent is JetFunctionLiteral) {
                            if (!parent.isMultiLine()) {
                                parent.addBefore(newLine, containingElement)
                                parent.addAfter(newLine, containingElement)
                            }

                            containingElement.addBefore(declaration, containingElement.getFirstChild()!!) as JetNamedDeclaration
                        }
                        else containingElement.addAfter(declaration, containingElement.getLBrace()!!) as JetNamedDeclaration
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

        private fun getTypeParameterRenames(scope: JetScope): Map<TypeParameterDescriptor, String> {
            val allTypeParametersNotInScope = LinkedHashSet<TypeParameterDescriptor>()

            mandatoryTypeParametersAsCandidates.stream()
                    .plus(callableInfo.parameterInfos.stream().flatMap { typeCandidates[it.typeInfo]!!.stream() })
                    .flatMap { it.typeParameters.stream() }
                    .toCollection(allTypeParametersNotInScope)

            if (!skipReturnType) {
                computeTypeCandidates(callableInfo.returnTypeInfo).stream().flatMapTo(allTypeParametersNotInScope) { it.typeParameters.stream() }
            }

            val validator = CollectingValidator { scope.getClassifier(Name.identifier(it)) == null }
            val typeParameterNames = allTypeParametersNotInScope.map { validator.validateName(it.getName().asString()) }

            return allTypeParametersNotInScope.zip(typeParameterNames).toMap()
        }

        private fun setupTypeReferencesForShortening(declaration: JetNamedDeclaration,
                                                     parameterTypeExpressions: List<TypeExpression>): List<JetElement> {
            val typeRefsToShorten = ArrayList<JetElement>()

            if (config.isExtension) {
                val receiverTypeRef = JetPsiFactory(declaration).createType(receiverTypeCandidate!!.theType.renderLong(typeParameterNameMap))
                replaceWithLongerName(receiverTypeRef, receiverTypeCandidate.theType)

                val funcReceiverTypeRef = (declaration as? JetCallableDeclaration)?.getReceiverTypeReference()
                if (funcReceiverTypeRef != null) {
                    typeRefsToShorten.add(funcReceiverTypeRef)
                }
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
            assert(valueParameters.size == parameterTypeExpressions.size)
            for ((i, parameter) in valueParameters.stream().withIndices()) {
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
            parameterIndicesToShorten.stream()
                    .map { expandedValueParameters[it].getTypeReference() }
                    .filterNotNullTo(typeRefsToShorten)

            return typeRefsToShorten
        }

        private fun setupFunctionBody(func: JetNamedFunction) {
            val oldBody = func.getBodyExpression() ?: return

            val fileTemplate = FileTemplateManager.getInstance()!!.getCodeTemplate(TEMPLATE_FROM_USAGE_FUNCTION_BODY)
            val properties = Properties()
            properties.setProperty(FileTemplate.ATTRIBUTE_RETURN_TYPE, if (skipReturnType) "Unit" else func.getTypeReference()!!.getText())
            receiverClassDescriptor?.let {
                properties.setProperty(FileTemplate.ATTRIBUTE_CLASS_NAME, DescriptorUtils.getFqName(it).asString())
                properties.setProperty(FileTemplate.ATTRIBUTE_SIMPLE_CLASS_NAME, it.getName().asString())
            }
            properties.setProperty(ATTRIBUTE_FUNCTION_NAME, callableInfo.name)

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

            oldBody.replace(JetPsiFactory(func).createFunctionBody(bodyText))
        }

        private fun setupCallTypeArguments(callElement: JetCallElement, typeParameters: List<TypeParameterDescriptor>) {
            val oldTypeArgumentList = callElement.getTypeArgumentList() ?: return
            val renderedTypeArgs = typeParameters.map { typeParameter ->
                val type = substitutions.first { it.byType.getConstructor().getDeclarationDescriptor() == typeParameter }.forType
                IdeDescriptorRenderers.SOURCE_CODE.renderType(type)
            }
            if (renderedTypeArgs.isEmpty()) {
                oldTypeArgumentList.delete()
            }
            else {
                oldTypeArgumentList.replace(JetPsiFactory(callElement).createTypeArguments(renderedTypeArgs.joinToString(", ", "<", ">")))
                elementsToShorten.add(callElement.getTypeArgumentList())
            }
        }

        private fun setupReturnTypeTemplate(builder: TemplateBuilder, declaration: JetNamedDeclaration): TypeExpression? {
            val candidates = typeCandidates[callableInfo.returnTypeInfo]!!
            if (candidates.isEmpty()) return null

            val elementToReplace: JetElement?
            val expression: TypeExpression
            when (declaration) {
                is JetCallableDeclaration -> {
                    elementToReplace = declaration.getTypeReference()
                    expression = TypeExpression.ForTypeReference(candidates)
                }
                is JetClassOrObject -> {
                    elementToReplace = declaration.getDelegationSpecifiers().firstOrNull()
                    expression = TypeExpression.ForDelegationSpecifier(candidates)
                }
                else -> throw AssertionError("Unexpected declaration kind: ${declaration.getText()}")
            }
            if (elementToReplace == null) return null

            if (candidates.size == 1) {
                builder.replaceElement(elementToReplace, (expression.calculateResult(null) as TextResult).getText())
                return null
            }

            builder.replaceElement(elementToReplace, expression)
            return expression
        }

        private fun setupValVarTemplate(builder: TemplateBuilder, property: JetProperty) {
            if (!(callableInfo as PropertyInfo).writable) {
                builder.replaceElement(property.getValOrVarNode().getPsi()!!, ValVarExpression)
            }
        }

        private fun setupTypeParameterListTemplate(
                builder: TemplateBuilderImpl,
                declaration: JetNamedDeclaration
        ): TypeParameterListExpression? {
            when (declaration) {
                is JetObjectDeclaration -> return null
                !is JetTypeParameterListOwner -> throw AssertionError("Unexpected declaration kind: ${declaration.getText()}")
            }

            val typeParameterList = (declaration as JetTypeParameterListOwner).getTypeParameterList() ?: return null

            val typeParameterMap = HashMap<String, List<RenderedTypeParameter>>()

            val mandatoryTypeParameters = ArrayList<RenderedTypeParameter>()
            //receiverTypeCandidate?.let { mandatoryTypeParameters.addAll(it.renderedTypeParameters!!) }
            mandatoryTypeParametersAsCandidates.stream().flatMapTo(mandatoryTypeParameters) { it.renderedTypeParameters!!.stream() }

            callableInfo.parameterInfos.stream().flatMap { typeCandidates[it.typeInfo]!!.stream() }.forEach {
                typeParameterMap[it.renderedType!!] = it.renderedTypeParameters!!
            }

            if (declaration.getReturnTypeReference() != null) {
                typeCandidates[callableInfo.returnTypeInfo]!!.forEach {
                    typeParameterMap[it.renderedType!!] = it.renderedTypeParameters!!
                }
            }

            val expression = TypeParameterListExpression(
                    mandatoryTypeParameters,
                    typeParameterMap,
                    callableInfo.kind != CallableKind.CONSTRUCTOR
            )
            builder.replaceElement(typeParameterList, expression, false)
            return expression
        }

        private fun setupParameterTypeTemplates(builder: TemplateBuilder, parameterList: List<JetParameter>): List<TypeExpression> {
            assert(parameterList.size == callableInfo.parameterInfos.size)

            val typeParameters = ArrayList<TypeExpression>()
            for ((parameter, jetParameter) in callableInfo.parameterInfos.zip(parameterList)) {
                val parameterTypeExpression = TypeExpression.ForTypeReference(typeCandidates[parameter.typeInfo]!!)
                val parameterTypeRef = jetParameter.getTypeReference()!!
                builder.replaceElement(parameterTypeRef, parameterTypeExpression)

                // add parameter name to the template
                val possibleNamesFromExpression = parameter.typeInfo.possibleNamesFromExpression
                val preferredName = parameter.preferredName
                val possibleNames = if (preferredName != null) {
                    array(preferredName, *possibleNamesFromExpression)
                }
                else {
                    possibleNamesFromExpression
                }

                // figure out suggested names for each type option
                val parameterTypeToNamesMap = HashMap<String, Array<String>>()
                typeCandidates[parameter.typeInfo]!!.forEach { typeCandidate ->
                    val suggestedNames = JetNameSuggester.suggestNamesForType(typeCandidate.theType, EmptyValidator)
                    parameterTypeToNamesMap[typeCandidate.renderedType!!] = suggestedNames
                }

                // add expression to builder
                val parameterNameExpression = ParameterNameExpression(possibleNames, parameterTypeToNamesMap)
                val parameterNameIdentifier = jetParameter.getNameIdentifier()!!
                builder.replaceElement(parameterNameIdentifier, parameterNameExpression)

                typeParameters.add(parameterTypeExpression)
            }
            return typeParameters
        }

        private fun replaceWithLongerName(typeRef: JetTypeReference, theType: JetType) {
            val fullyQualifiedReceiverTypeRef = JetPsiFactory(typeRef).createType(theType.renderLong(typeParameterNameMap))
            typeRef.replace(fullyQualifiedReceiverTypeRef)
        }

        private fun transformToJavaMemberIfApplicable(declaration: JetNamedDeclaration): Boolean {
            fun convertToJava(targetClass: PsiClass): PsiMember? {
                val psiFactory = JetPsiFactory(declaration)

                psiFactory.createPackageDirectiveIfNeeded(config.currentFile.getPackageFqName())?.let {
                    declaration.getContainingFile().addBefore(it, null)
                }

                val adjustedDeclaration = when (declaration) {
                    is JetNamedFunction, is JetProperty -> {
                        val klass = psiFactory.createClass("class Foo {}")
                        klass.getBody().add(declaration)
                        (declaration.replace(klass) as JetClass).getBody().getDeclarations().first()
                    }
                    else -> declaration
                }

                return when (adjustedDeclaration) {
                    is JetNamedFunction -> {
                        createJavaMethod(adjustedDeclaration, targetClass)
                    }
                    is JetProperty -> {
                        createJavaField(adjustedDeclaration, targetClass)
                    }
                    is JetClass -> {
                        createJavaClass(adjustedDeclaration, targetClass)
                    }
                    else -> null
                }
            }

            if (config.isExtension || receiverClassDescriptor !is JavaClassDescriptor) return false

            val targetClass = DescriptorToSourceUtils.classDescriptorToDeclaration(receiverClassDescriptor) as? PsiClass
            if (targetClass == null || !targetClass.canRefactor()) return false

            val project = declaration.getProject()

            val newJavaMember = convertToJava(targetClass) ?: return false

            val modifierList = newJavaMember.getModifierList()
            if (newJavaMember is PsiMethod || newJavaMember is PsiClass) {
                modifierList.setModifierProperty(PsiModifier.FINAL, false)
            }

            val needStatic = when (callableInfo) {
                is ConstructorInfo -> with(callableInfo.classInfo) {
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
                is PsiField -> targetEditor.getCaretModel().moveToOffset(newJavaMember.getTextRange().getEndOffset() - 1)
                is PsiClass -> {
                    val constructor = newJavaMember.getConstructors().firstOrNull()
                    val superStatement = constructor?.getBody()?.getStatements()?.firstOrNull() as? PsiExpressionStatement
                    val superCall = superStatement?.getExpression() as? PsiMethodCallExpression
                    if (superCall != null) {
                        val lParen = superCall.getArgumentList().getFirstChild()
                        targetEditor.getCaretModel().moveToOffset(lParen.getTextRange().getEndOffset())
                    }
                    else {
                        targetEditor.getCaretModel().moveToOffset(newJavaMember.getTextRange().getStartOffset())
                    }
                }
            }
            targetEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE)

            return true
        }

        private fun setupEditor(declaration: JetNamedDeclaration) {
            val caretModel = containingFileEditor.getCaretModel()

            caretModel.moveToOffset(declaration.getNameIdentifier().getTextRange().getEndOffset())

            fun positionBetween(left: PsiElement, right: PsiElement) {
                val from = left.siblings(withItself = false, forward = true).firstOrNull { it !is PsiWhiteSpace } ?: return
                val to = right.siblings(withItself = false, forward = false).firstOrNull { it !is PsiWhiteSpace } ?: return
                val startOffset = from.getTextRange().getStartOffset()
                val endOffset = to.getTextRange().getEndOffset()
                caretModel.moveToOffset(endOffset)
                containingFileEditor.getSelectionModel().setSelection(startOffset, endOffset)
            }

            when (declaration) {
                is JetNamedFunction -> {
                    (declaration.getBodyExpression() as? JetBlockExpression)?.let { positionBetween(it.getLBrace(), it.getRBrace()) }
                }
                is JetClassOrObject -> {
                    caretModel.moveToOffset(declaration.getTextRange().getStartOffset())
                }
                is JetProperty -> {
                    if (!declaration.hasInitializer()) {
                        caretModel.moveToOffset(declaration.getTextRange().getEndOffset())
                    }
                }
            }
            containingFileEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE)
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

            val declaration = declarationPointer.getElement()

            val builder = TemplateBuilderImpl(jetFileToEdit)
            if (declaration is JetProperty) {
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
                for (i in 0..(callableInfo.parameterInfos.size - 1)) {
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
                        if (brokenOff && !ApplicationManager.getApplication().isUnitTestMode()) {
                            NavigationUtil.activateFileWithPsiElement(config.originalElement)
                            return
                        }

                        // file templates
                        val newDeclaration = if (templateImpl.getSegmentsCount() > 0) {
                            PsiTreeUtil.findElementOfClassAtOffset(jetFileToEdit, templateImpl.getSegmentOffset(0), declaration.javaClass, false)!!
                        }
                        else declarationPointer.getElement()!!

                        runWriteAction {
                            // file templates
                            if (newDeclaration is JetNamedFunction) {
                                setupFunctionBody(newDeclaration)
                            }

                            val callElement = config.originalElement as? JetCallElement
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
                        release()
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
            if (!ApplicationManager.getApplication().isUnitTestMode() && dialogWithEditor != null && !released) {
                dialogWithEditor.show()
            }
        }

        private fun release() {
            if (released) return
            dialogWithEditor?.let {
                jetFileToEdit.delete()
                released = true
            }
        }
    }
}

private fun JetNamedDeclaration.getValueParameters(): List<JetParameter> {
    return when (this) {
               is JetCallableDeclaration -> getValueParameterList()
               is JetClass -> getPrimaryConstructorParameterList()
               is JetObjectDeclaration -> null
               else -> throw AssertionError("Unexpected declaration kind: ${getText()}")
           }?.getParameters() ?: Collections.emptyList()
}

private fun JetNamedDeclaration.getReturnTypeReference(): JetTypeReference? {
    return when (this) {
        is JetCallableDeclaration -> getTypeReference()
        is JetClassOrObject -> getDelegationSpecifiers().firstOrNull()?.getTypeReference()
        else -> throw AssertionError("Unexpected declaration kind: ${getText()}")
    }
}

fun CallableBuilderConfiguration.createBuilder(): CallableBuilder = CallableBuilder(this)
