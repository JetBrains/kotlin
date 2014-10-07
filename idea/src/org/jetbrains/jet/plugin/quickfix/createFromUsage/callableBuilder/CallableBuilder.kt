/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder

import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.codeInsight.template.*
import com.intellij.codeInsight.template.impl.TemplateImpl
import com.intellij.codeInsight.template.impl.Variable
import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.types.*
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.plugin.codeInsight.ShortenReferences
import org.jetbrains.jet.plugin.refactoring.JetNameSuggester
import kotlin.properties.Delegates
import java.util.LinkedHashSet
import java.util.Collections
import java.util.HashMap
import java.util.ArrayList
import java.util.Properties
import org.jetbrains.jet.plugin.caches.resolve.getAnalysisResults
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils
import org.jetbrains.jet.plugin.refactoring.EmptyValidator
import org.jetbrains.jet.plugin.refactoring.CollectingValidator
import org.jetbrains.jet.plugin.util.isUnit
import com.intellij.util.ArrayUtil
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.plugin.quickfix.createFromUsage.createFunction
import org.jetbrains.jet.plugin.util.application.runWriteAction
import org.jetbrains.jet.plugin.refactoring.isMultiLine
import org.jetbrains.jet.plugin.intentions.declarations.DeclarationUtils

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
    var typeParameterNames: Array<String>? = null
        private set

    fun render(typeParameterNameMap: Map<TypeParameterDescriptor, String>) {
        renderedType = theType.renderShort(typeParameterNameMap);
        typeParameterNames = typeParameters.map { typeParameterNameMap[it]!! }.copyToArray()
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
}

class CallableBuilderConfiguration(
        val callableInfo: CallableInfo,
        val originalExpression: JetExpression,
        val currentFile: JetFile,
        val currentEditor: Editor
)

trait CallablePlacement {
    class WithReceiver(val receiverTypeCandidate: TypeCandidate): CallablePlacement
    class NoReceiver(val containingElement: JetElement): CallablePlacement
}

class CallableBuilder(val config: CallableBuilderConfiguration) {
    private var finished: Boolean = false

    val currentFileContext: BindingContext
    val currentFileModule: ModuleDescriptor

    private val typeCandidates = HashMap<TypeInfo, List<TypeCandidate>>();

    {
        val exhaust = config.currentFile.getAnalysisResults()
        currentFileContext = exhaust.getBindingContext()
        currentFileModule = exhaust.getModuleDescriptor()
    }

    public var placement: CallablePlacement by Delegates.notNull()

    fun computeTypeCandidates(typeInfo: TypeInfo): List<TypeCandidate> =
            typeCandidates.getOrPut(typeInfo) { typeInfo.getPossibleTypes(this).map { TypeCandidate(it) } }

    fun computeTypeCandidates(
            typeInfo: TypeInfo,
            substitutions: Array<JetTypeSubstitution>,
            scope: JetScope): List<TypeCandidate> {
        if (typeInfo is TypeInfo.ByType && typeInfo.keepUnsubstituted) return computeTypeCandidates(typeInfo)
        return typeCandidates.getOrPut(typeInfo) {
            val types = typeInfo.getPossibleTypes(this).reverse()

            val newTypes = LinkedHashSet(types)
            for (substitution in substitutions) {
                // each substitution can be applied or not, so we offer all options
                val toAdd = newTypes.map { theType -> theType.substitute(substitution, typeInfo.variance) }
                // substitution.byType are type arguments, but they cannot already occur in the type before substitution
                val toRemove = newTypes.filter { theType -> substitution.byType in theType }

                newTypes.addAll(toAdd)
                newTypes.removeAll(toRemove)
            }

            if (newTypes.empty) {
                newTypes.add(KotlinBuiltIns.getInstance().getAnyType())
            }

            newTypes.map { TypeCandidate(it, scope) }.reverse()
        }
    }

    fun build() {
        try {
            if (finished) throw IllegalStateException("Current builder has already finished")

            val context = Context()
            runWriteAction { context.buildAndRunTemplate() }
        }
        finally {
            finished = true
        }
    }

    private inner class Context {
        val isUnit: Boolean
        val isExtension: Boolean
        val containingFile: JetFile
        val containingFileEditor: Editor
        val containingElement: JetElement
        val receiverClassDescriptor: ClassDescriptor?
        val typeParameterNameMap: Map<TypeParameterDescriptor, String>
        val receiverTypeCandidate: TypeCandidate?

        {
            // gather relevant information

            val placement = placement
            when {
                placement is CallablePlacement.NoReceiver -> {
                    receiverClassDescriptor = null
                    isExtension = false
                    containingElement = placement.containingElement
                }
                placement is CallablePlacement.WithReceiver -> {
                    receiverClassDescriptor = DescriptorUtils.getClassDescriptorForType(placement.receiverTypeCandidate.theType)
                    val classDeclaration = receiverClassDescriptor?.let { DescriptorToSourceUtils.classDescriptorToDeclaration(it) }
                    isExtension = !(classDeclaration is JetClassOrObject && classDeclaration.isWritable())
                    containingElement = if (isExtension) config.currentFile else classDeclaration as JetElement
                }
                else -> throw IllegalArgumentException("Unexpected placement: $placement")
            }
            val receiverType = receiverClassDescriptor?.getDefaultType()

            containingFile = containingElement.getContainingJetFile()
            if (containingFile != config.currentFile) {
                NavigationUtil.activateFileWithPsiElement(containingElement)
                containingFileEditor = FileEditorManager.getInstance(config.currentFile.getProject())!!.getSelectedTextEditor()!!
            }
            else {
                containingFileEditor = config.currentEditor
            }

            isUnit = config.callableInfo.returnTypeInfo.let { it is TypeInfo.ByType && it.theType.isUnit() }

            val scope = if (isExtension || receiverClassDescriptor == null) {
                currentFileModule.getPackage(config.currentFile.getPackageFqName())!!.getMemberScope()
            }
            else {
                (receiverClassDescriptor as ClassDescriptorWithResolutionScopes).getScopeForMemberDeclarationResolution()
            }

            // figure out type substitutions for type parameters
            val classTypeParameters = receiverType?.getArguments() ?: Collections.emptyList()
            val ownerTypeArguments = (placement as? CallablePlacement.WithReceiver)?.receiverTypeCandidate?.theType?.getArguments()
                                     ?: Collections.emptyList()
            assert(ownerTypeArguments.size == classTypeParameters.size)
            val substitutions = ownerTypeArguments.zip(classTypeParameters).map {
                JetTypeSubstitution(it.first.getType(), it.second.getType())
            }.copyToArray()
            config.callableInfo.parameterInfos.forEach { parameter -> computeTypeCandidates(parameter.typeInfo, substitutions, scope) }
            if (!isUnit) {
                computeTypeCandidates(config.callableInfo.returnTypeInfo, substitutions, scope)
            }

            // now that we have done substitutions, we can throw it away
            receiverTypeCandidate = receiverType?.let { TypeCandidate(it, scope) }

            // figure out type parameter renames to avoid conflicts
            typeParameterNameMap = getTypeParameterRenames(scope)
            config.callableInfo.parameterInfos.forEach { renderTypeCandidates(it.typeInfo, typeParameterNameMap) }
            if (!isUnit) {
                renderTypeCandidates(config.callableInfo.returnTypeInfo, typeParameterNameMap)
            }
            receiverTypeCandidate?.render(typeParameterNameMap)
        }

        private fun renderTypeCandidates(
                typeInfo: TypeInfo,
                typeParameterNameMap: Map<TypeParameterDescriptor, String>
        ) {
            typeCandidates[typeInfo]?.forEach { it.render(typeParameterNameMap) }
        }

        private fun createDeclarationSkeleton(): JetCallableDeclaration {
            with (config) {
                val assignmentToReplace =
                        if (containingElement is JetBlockExpression && (config.callableInfo as? PropertyInfo)?.writable ?: false) {
                            originalExpression as JetBinaryExpression
                        }
                        else null

                val ownerTypeString = if (isExtension) "${receiverTypeCandidate!!.renderedType!!}." else ""
                val paramList = when (callableInfo.kind) {
                    CallableKind.FUNCTION ->
                        "(${(callableInfo as FunctionInfo).parameterInfos.indices.map { i -> "p$i: Any" }.joinToString(", ")})"
                    CallableKind.PROPERTY ->
                        ""
                }
                val returnTypeString = if (isUnit || assignmentToReplace != null) "" else ": Any"
                val header = "$ownerTypeString${callableInfo.name}$paramList$returnTypeString"

                val psiFactory = JetPsiFactory(currentFile)

                val declaration = when (callableInfo.kind) {
                    CallableKind.FUNCTION -> psiFactory.createFunction("fun $header {}")
                    CallableKind.PROPERTY -> {
                        val valVar = if ((callableInfo as PropertyInfo).writable) "var" else "val"
                        psiFactory.createProperty("$valVar $header")
                    }
                }

                if (assignmentToReplace != null) {
                    (declaration as JetProperty).setInitializer(assignmentToReplace.getRight())
                    return assignmentToReplace.replace(declaration) as JetCallableDeclaration
                }

                val newLine = psiFactory.createNewLine()

                fun prepend(element: PsiElement, elementBeforeStart: PsiElement, skipInitial: Boolean): PsiElement {
                    val parent = elementBeforeStart.getParent()!!
                    val anchor =
                            if (!skipInitial && elementBeforeStart !is PsiWhiteSpace) {
                                elementBeforeStart
                            }
                            else {
                                PsiTreeUtil.skipSiblingsForward(elementBeforeStart, javaClass<PsiWhiteSpace>())
                            }
                    val addedElement = parent.addBefore(element, anchor)!!
                    parent.addAfter(newLine, addedElement)
                    parent.addAfter(newLine, addedElement)
                    return addedElement
                }

                fun append(element: PsiElement, elementAfterEnd: PsiElement, skipInitial: Boolean): PsiElement {
                    val parent = elementAfterEnd.getParent()!!
                    val anchor =
                            if (!skipInitial && elementAfterEnd !is PsiWhiteSpace) {
                                elementAfterEnd
                            }
                            else {
                                PsiTreeUtil.skipSiblingsBackward(elementAfterEnd, javaClass<PsiWhiteSpace>())
                            }
                    val addedElement = parent.addAfter(element, anchor)!!
                    if (anchor?.getNode()?.getElementType() != JetTokens.LBRACE) {
                        parent.addAfter(newLine, anchor)
                        parent.addAfter(newLine, anchor)
                    }
                    return addedElement
                }

                when (containingElement) {
                    is JetFile -> return append(declaration, containingElement.getLastChild()!!, false) as JetCallableDeclaration

                    is JetClassOrObject -> {
                        var classBody = containingElement.getBody()
                        if (classBody == null) {
                            classBody = containingElement.add(psiFactory.createEmptyClassBody()) as JetClassBody
                            containingElement.addBefore(psiFactory.createWhiteSpace(), classBody)
                        }

                        if (declaration is JetNamedFunction) {
                            val rBrace = classBody!!.getRBrace()
                            return (rBrace?.let { append(declaration, it, true) }
                                    ?: append(declaration, classBody!!.getLastChild()!!, false)) as JetCallableDeclaration
                        }
                        return prepend(declaration, classBody!!.getLBrace()!!, true) as JetCallableDeclaration
                    }

                    is JetBlockExpression -> {
                        val parent = containingElement.getParent()
                        if (parent is JetFunctionLiteral) {
                            if (!parent.isMultiLine()) {
                                parent.addBefore(newLine, containingElement)
                                parent.addAfter(newLine, containingElement)
                            }

                            return prepend(declaration, containingElement.getFirstChild()!!, false) as JetCallableDeclaration
                        }
                        return prepend(declaration, containingElement.getLBrace()!!, true) as JetCallableDeclaration
                    }

                    else -> throw AssertionError("Invalid containing element: ${containingElement.getText()}")
                }
            }
        }

        private fun getTypeParameterRenames(scope: JetScope): Map<TypeParameterDescriptor, String> {
            val allTypeParametersNotInScope = LinkedHashSet<TypeParameterDescriptor>()

            allTypeParametersNotInScope.addAll(receiverTypeCandidate?.typeParameters?.toList() ?: Collections.emptyList())

            config.callableInfo.parameterInfos.stream()
                    .flatMap { typeCandidates[it.typeInfo]!!.stream() }
                    .flatMap { it.typeParameters.stream() }
                    .toCollection(allTypeParametersNotInScope)

            if (!isUnit) {
                computeTypeCandidates(config.callableInfo.returnTypeInfo).stream().flatMapTo(allTypeParametersNotInScope) { it.typeParameters.stream() }
            }

            val validator = CollectingValidator { scope.getClassifier(Name.identifier(it)) == null }
            val typeParameterNames = allTypeParametersNotInScope.map { validator.validateName(it.getName().asString()) }

            return allTypeParametersNotInScope.zip(typeParameterNames).toMap()
        }

        private fun setupTypeReferencesForShortening(
                declaration: JetCallableDeclaration,
                typeRefsToShorten: MutableList<JetTypeReference>, parameterTypeExpressions: List<TypeExpression>,
                returnTypeExpression: TypeExpression?) {
            if (isExtension) {
                val receiverTypeRef = JetPsiFactory(declaration).createType(receiverTypeCandidate!!.theType.renderLong(typeParameterNameMap))
                replaceWithLongerName(receiverTypeRef, receiverTypeCandidate.theType)

                val funcReceiverTypeRef = declaration.getReceiverTypeReference()
                if (funcReceiverTypeRef != null) {
                    typeRefsToShorten.add(funcReceiverTypeRef)
                }
            }

            if (returnTypeExpression != null) {
                val returnTypeRef = declaration.getTypeReference()
                if (returnTypeRef != null) {
                    val returnType = returnTypeExpression.getTypeFromSelection(
                            returnTypeRef.getText()
                            ?: throw AssertionError("Expression for return type shouldn't be empty: declaration = ${declaration.getText()}")
                    )
                    if (returnType != null) {
                        // user selected a given type
                        replaceWithLongerName(returnTypeRef, returnType)
                        typeRefsToShorten.add(declaration.getTypeReference()!!)
                    }
                }
            }

            val valueParameters = declaration.getValueParameterList()?.getParameters() ?: Collections.emptyList<JetParameter>()
            val parameterIndicesToShorten = ArrayList<Int>()
            assert(valueParameters.size == parameterTypeExpressions.size)
            for ((i, parameter) in valueParameters.stream().withIndices()) {
                val parameterTypeRef = parameter.getTypeReference()
                if (parameterTypeRef != null) {
                    val parameterType = parameterTypeExpressions[i].getTypeFromSelection(
                            parameterTypeRef.getText()
                            ?: throw AssertionError("Expression for parameter type shouldn't be empty: declaration = ${declaration.getText()}")
                    )
                    if (parameterType != null) {
                        replaceWithLongerName(parameterTypeRef, parameterType)
                        parameterIndicesToShorten.add(i)
                    }
                }
            }

            declaration.getValueParameterList()?.getParameters()?.let { expandedValueParameters ->
                parameterIndicesToShorten.stream()
                        .map { expandedValueParameters[it].getTypeReference() }
                        .filterNotNullTo(typeRefsToShorten)
            }
        }

        private fun setupFunctionBody(func: JetNamedFunction) {
            val fileTemplate = FileTemplateManager.getInstance()!!.getCodeTemplate(TEMPLATE_FROM_USAGE_FUNCTION_BODY)
            val properties = Properties()
            properties.setProperty(FileTemplate.ATTRIBUTE_RETURN_TYPE, if (isUnit) "Unit" else func.getTypeReference()!!.getText())
            receiverClassDescriptor?.let {
                properties.setProperty(FileTemplate.ATTRIBUTE_CLASS_NAME, DescriptorUtils.getFqName(it).asString())
                properties.setProperty(FileTemplate.ATTRIBUTE_SIMPLE_CLASS_NAME, it.getName().asString())
            }
            properties.setProperty(ATTRIBUTE_FUNCTION_NAME, config.callableInfo.name)

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

            val newBodyExpression = JetPsiFactory(func).createFunctionBody(bodyText)
            func.getBodyExpression()!!.replace(newBodyExpression)
        }

        private fun setupReturnTypeTemplate(builder: TemplateBuilder, declaration: JetCallableDeclaration): TypeExpression? {
            val returnTypeRef = declaration.getTypeReference() ?: return null
            val candidates = typeCandidates[config.callableInfo.returnTypeInfo]!!
            return when (candidates.size) {
                0 -> null

                1 -> {
                    builder.replaceElement(returnTypeRef, candidates.first().renderedType!!)
                    null
                }

                else -> {
                    val returnTypeExpression = TypeExpression(candidates)
                    builder.replaceElement(returnTypeRef, returnTypeExpression)
                    returnTypeExpression
                }
            }
        }

        private fun setupValVarTemplate(builder: TemplateBuilder, property: JetProperty) {
            if (!(config.callableInfo as PropertyInfo).writable) {
                builder.replaceElement(property.getValOrVarNode().getPsi()!!, ValVarExpression)
            }
        }

        private fun setupTypeParameterListTemplate(builder: TemplateBuilderImpl, declaration: JetCallableDeclaration): TypeParameterListExpression {
            val typeParameterMap = HashMap<String, Array<String>>()
            val receiverTypeParameterNames = receiverTypeCandidate?.let { it.typeParameterNames!! } ?: ArrayUtil.EMPTY_STRING_ARRAY

            config.callableInfo.parameterInfos.stream().flatMap { typeCandidates[it.typeInfo]!!.stream() }.forEach {
                typeParameterMap[it.renderedType!!] = it.typeParameterNames!!
            }

            if (declaration.getTypeReference() != null) {
                typeCandidates[config.callableInfo.returnTypeInfo]!!.forEach {
                    typeParameterMap[it.renderedType!!] = it.typeParameterNames!!
                }
            }
            // ((3, 3) is after "fun")
            builder.replaceElement(declaration, TextRange.create(3, 3), TYPE_PARAMETER_LIST_VARIABLE_NAME, null, false)
            return TypeParameterListExpression(receiverTypeParameterNames, typeParameterMap)
        }

        private fun setupParameterTypeTemplates(builder: TemplateBuilder, parameterList: List<JetParameter>): List<TypeExpression> {
            assert(parameterList.size == config.callableInfo.parameterInfos.size)

            val typeParameters = ArrayList<TypeExpression>()
            for ((parameter, jetParameter) in config.callableInfo.parameterInfos.zip(parameterList)) {
                val parameterTypeExpression = TypeExpression(typeCandidates[parameter.typeInfo]!!)
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

        fun buildAndRunTemplate() {
            val declaration = createDeclarationSkeleton()
            val project = declaration.getProject()

            // build templates
            PsiDocumentManager.getInstance(project).commitAllDocuments()
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(containingFileEditor.getDocument())

            val caretModel = containingFileEditor.getCaretModel()
            caretModel.moveToOffset(containingFile.getNode().getStartOffset())

            val builder = TemplateBuilderImpl(containingFile)
            if (declaration is JetProperty) {
                setupValVarTemplate(builder, declaration)
            }
            val returnTypeExpression = if (isUnit) null else setupReturnTypeTemplate(builder, declaration)
            val parameterTypeExpressions =
                    setupParameterTypeTemplates(builder, declaration.getValueParameterList()?.getParameters() ?: Collections.emptyList())

            // add a segment for the parameter list
            // Note: because TemplateBuilderImpl does not have a replaceElement overload that takes in both a TextRange and alwaysStopAt, we
            // need to create the segment first and then hack the Expression into the template later. We use this template to update the type
            // parameter list as the user makes selections in the parameter types, and we need alwaysStopAt to be false so the user can't tab to
            // it.
            val expression = setupTypeParameterListTemplate(builder, declaration)

            // the template built by TemplateBuilderImpl is ordered by element position, but we want types to be first, so hack it
            val templateImpl = builder.buildInlineTemplate() as TemplateImpl
            val variables = templateImpl.getVariables()!!
            for (i in 0..(config.callableInfo.parameterInfos.size - 1)) {
                Collections.swap(variables, i * 2, i * 2 + 1)
            }

            // fix up the template to include the expression for the type parameter list
            variables.add(Variable(TYPE_PARAMETER_LIST_VARIABLE_NAME, expression, expression, false, true))

            // TODO: Disabled shortening names because it causes some tests fail. Refactor code to use automatic reference shortening
            templateImpl.setToShortenLongNames(false)

            // run the template
            TemplateManager.getInstance(project).startTemplate(containingFileEditor, templateImpl, object : TemplateEditingAdapter() {
                override fun templateFinished(template: Template?, brokenOff: Boolean) {
                    // file templates
                    val offset = templateImpl.getSegmentOffset(0)
                    val newDeclaration = PsiTreeUtil.findElementOfClassAtOffset(
                            containingFile, offset, javaClass<JetCallableDeclaration>(), false
                    )!!
                    val typeRefsToShorten = ArrayList<JetTypeReference>()

                    ApplicationManager.getApplication()!!.runWriteAction {
                        // file templates
                        if (newDeclaration is JetNamedFunction) {
                            setupFunctionBody(newDeclaration)
                        }

                        // change short type names to fully qualified ones (to be shortened below)
                        setupTypeReferencesForShortening(newDeclaration, typeRefsToShorten, parameterTypeExpressions, returnTypeExpression)
                        ShortenReferences.process(typeRefsToShorten)
                    }
                }
            })
        }

    }
}

fun CallableBuilderConfiguration.createBuilder(): CallableBuilder = CallableBuilder(this)
