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

package org.jetbrains.jet.plugin.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.codeInsight.template.*
import com.intellij.codeInsight.template.impl.TemplateImpl
import com.intellij.codeInsight.template.impl.Variable
import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.PopupChooserBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.psi.SearchUtils
import com.intellij.ui.components.JBList
import com.intellij.util.ArrayUtil
import com.intellij.util.IncorrectOperationException
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.diagnostics.Diagnostic
import org.jetbrains.jet.lang.diagnostics.Errors
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.types.*
import org.jetbrains.jet.lang.types.checker.JetTypeChecker
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.plugin.JetBundle
import org.jetbrains.jet.plugin.codeInsight.DescriptorToDeclarationUtil
import org.jetbrains.jet.plugin.codeInsight.ShortenReferences
import org.jetbrains.jet.plugin.refactoring.JetNameSuggester
import org.jetbrains.jet.plugin.refactoring.JetNameValidator
import org.jetbrains.jet.plugin.references.JetSimpleNameReference
import javax.swing.*
import java.util.regex.Pattern
import kotlin.properties.Delegates
import java.util.LinkedHashSet
import java.util.Collections
import java.util.HashSet
import java.util.HashMap
import java.util.ArrayList
import java.util.Properties
import org.jetbrains.jet.plugin.caches.resolve.getAnalysisResults
import org.jetbrains.jet.plugin.caches.resolve.getBindingContext
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils
import org.jetbrains.jet.lang.resolve.name.COMPONENT_FUNCTION_PATTERN

private val TYPE_PARAMETER_LIST_VARIABLE_NAME = "typeParameterList"
private val TEMPLATE_FROM_USAGE_FUNCTION_BODY = "New Kotlin Function Body.kt"
private val ATTRIBUTE_FUNCTION_NAME = "FUNCTION_NAME"

/**
 * Represents a single choice for a type (e.g. parameter type or return type).
 */
private class TypeCandidate(public val theType: JetType, scope: JetScope? = null) {
    public val typeParameters: Array<TypeParameterDescriptor>
    public var renderedType: String? = null
        private set
    public var typeParameterNames: Array<String>? = null
        private set

    public fun render(typeParameterNameMap: Map<TypeParameterDescriptor, String>) {
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

/**
 * Represents an element in the class selection list.
 */
private class ClassCandidate(public val typeCandidate: TypeCandidate, file: JetFile) {
    public val jetClass: JetClass = DescriptorToDeclarationUtil.getDeclaration(
            file, DescriptorUtils.getClassDescriptorForType(typeCandidate.theType)
    ) as JetClass
}

/**
 * Represents a concrete type or a set of types yet to be inferred from an expression.
 */
private fun TypeOrExpressionThereof(expressionOfType: JetExpression, variance: Variance): TypeOrExpressionThereof =
        TypeOrExpressionThereof(variance, expressionOfType, null)
private fun TypeOrExpressionThereof(theType: JetType, variance: Variance): TypeOrExpressionThereof =
        TypeOrExpressionThereof(variance, null, theType)

private class TypeOrExpressionThereof(private val variance: Variance,
                                      public val expressionOfType: JetExpression?,
                                      public val theType: JetType?) {
    public var typeCandidates: List<TypeCandidate>? = null
        private set

    public val possibleNamesFromExpression: Array<String> by Delegates.lazy {
        if (isType()) {
            ArrayUtil.EMPTY_STRING_ARRAY
        }
        else {
            JetNameSuggester.suggestNamesForExpression(
                    expressionOfType!!,
                    JetNameValidator.getEmptyValidator(expressionOfType.getProject())
            )
        }
    }

    public fun isType(): Boolean = (theType != null)

    private fun getPossibleTypes(context: BindingContext): List<JetType> {
        val types = ArrayList<JetType>()
        if (theType != null) {
            types.add(theType)
            if (variance == Variance.IN_VARIANCE) {
                types.addAll(TypeUtils.getAllSupertypes(theType))
            }
        }
        else {
            for (theType in expressionOfType!!.guessTypes(context)) {
                types.add(theType)
                if (variance == Variance.IN_VARIANCE) {
                    types.addAll(TypeUtils.getAllSupertypes(theType))
                }
            }
        }
        return types
    }

    public fun computeTypeCandidates(context: BindingContext) {
        typeCandidates = getPossibleTypes(context).map { TypeCandidate(it) }
    }

    public fun computeTypeCandidates(context: BindingContext, substitutions: Array<JetTypeSubstitution>, scope: JetScope) {
        val types = getPossibleTypes(context).reverse()

        val newTypes = LinkedHashSet<JetType>(types)
        for (substitution in substitutions) {
            // each substitution can be applied or not, so we offer all options
            val toAdd = newTypes.map { theType -> theType.substitute(substitution, variance) }
            // substitution.byType are type arguments, but they cannot already occur in the type before substitution
            val toRemove = newTypes.filter { theType -> substitution.byType in theType }

            newTypes.addAll(toAdd)
            newTypes.removeAll(toRemove)
        }

        if (newTypes.empty) {
            newTypes.add(KotlinBuiltIns.getInstance().getAnyType())
        }

        typeCandidates = newTypes.map { TypeCandidate(it, scope) }.reverse()
    }

    public fun renderTypeCandidates(typeParameterNameMap: Map<TypeParameterDescriptor, String>) {
        typeCandidates!!.forEach { it.render(typeParameterNameMap) }
    }
}

/**
 * Encapsulates information about a function parameter that is going to be created.
 */
private class Parameter(
        public val theType: TypeOrExpressionThereof,
        public val preferredName: String? = null
)

/**
 * Special <code>Expression</code> for parameter names based on its type.
 */
private class ParameterNameExpression(
        private val names: Array<String>,
        private val parameterTypeToNamesMap: Map<String, Array<String>>) : Expression() {
    {
        assert(names all { it.isNotEmpty() })
    }

    override fun calculateResult(context: ExpressionContext?): Result? {
        val lookupItems = calculateLookupItems(context)!!
        return TextResult(if (lookupItems.isEmpty()) "" else lookupItems.first().getLookupString())
    }

    override fun calculateQuickResult(context: ExpressionContext?) = calculateResult(context)

    override fun calculateLookupItems(context: ExpressionContext?): Array<LookupElement>? {
        context!!
        val names = LinkedHashSet<String>(this.names.toList())

        // find the parameter list
        val project = context.getProject()!!
        val offset = context.getStartOffset()
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        val editor = context.getEditor()!!
        val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument()) as JetFile
        val elementAt = file.findElementAt(offset)
        val func = PsiTreeUtil.getParentOfType(elementAt, javaClass<JetFunction>()) ?: return array<LookupElement>()
        val parameterList = func.getValueParameterList()!!

        // add names based on selected type
        val parameter = PsiTreeUtil.getParentOfType(elementAt, javaClass<JetParameter>())
        if (parameter != null) {
            val parameterTypeRef = parameter.getTypeReference()
            if (parameterTypeRef != null) {
                val suggestedNamesBasedOnType = parameterTypeToNamesMap[parameterTypeRef.getText()]
                if (suggestedNamesBasedOnType != null) {
                    names.addAll(suggestedNamesBasedOnType)
                }
            }
        }

        // remember other parameter names for later use
        val parameterNames = parameterList.getParameters().stream().map { jetParameter ->
            if (jetParameter == parameter) null else jetParameter.getName()
        }.filterNotNullTo(HashSet<String>())

        // add fallback parameter name
        if (names.isEmpty()) {
            names.add("arg")
        }

        // ensure there are no conflicts
        return names.map { name ->
            LookupElementBuilder.create(getNextAvailableName(name, parameterNames, null))
        }.copyToArray()
    }
}

/**
 * An <code>Expression</code> for type references.
 */
private class TypeExpression(public val theType: TypeOrExpressionThereof) : Expression() {
    private val cachedLookupElements: Array<LookupElement> = theType.typeCandidates!!.map {
        LookupElementBuilder.create(it, it.renderedType!!)
    }.copyToArray()

    override fun calculateResult(context: ExpressionContext?): Result {
        val lookupItems = calculateLookupItems(context)
        return TextResult(if (lookupItems.size == 0) "" else lookupItems[0].getLookupString())
    }

    override fun calculateQuickResult(context: ExpressionContext?) = calculateResult(context)

    override fun calculateLookupItems(context: ExpressionContext?) = cachedLookupElements

    public fun getTypeFromSelection(selection: String): JetType? {
        return theType.typeCandidates!!.firstOrNull { it.renderedType == selection }?.theType
    }
}

/**
 * A sort-of dummy <code>Expression</code> for parameter lists, to allow us to update the parameter list as the user makes selections.
 */
private class TypeParameterListExpression(private val typeParameterNamesFromReceiverType: Array<String>,
                                          private val parameterTypeToTypeParameterNamesMap: Map<String, Array<String>>) : Expression() {

    override fun calculateResult(context: ExpressionContext?): Result {
        context!!
        val project = context.getProject()!!
        val offset = context.getStartOffset()

        PsiDocumentManager.getInstance(project).commitAllDocuments()
        val editor = context.getEditor()!!
        val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument()) as JetFile
        val elementAt = file.findElementAt(offset)
        val func = PsiTreeUtil.getParentOfType(elementAt, javaClass<JetFunction>()) ?: return TextResult("")
        val parameters = func.getValueParameters()

        val typeParameterNames = LinkedHashSet<String>()
        typeParameterNames.addAll(typeParameterNamesFromReceiverType)
        for (parameter in parameters) {
            val parameterTypeRef = parameter.getTypeReference()
            if (parameterTypeRef != null) {
                val typeParameterNamesFromParameter = parameterTypeToTypeParameterNamesMap[parameterTypeRef.getText()]
                if (typeParameterNamesFromParameter != null) {
                    typeParameterNames.addAll(typeParameterNamesFromParameter)
                }
            }
        }
        val returnTypeRef = func.getReturnTypeRef()
        if (returnTypeRef != null) {
            val typeParameterNamesFromReturnType = parameterTypeToTypeParameterNamesMap[returnTypeRef.getText()]
            if (typeParameterNamesFromReturnType != null) {
                typeParameterNames.addAll(typeParameterNamesFromReturnType)
            }
        }

        return TextResult(if (typeParameterNames.empty) "" else typeParameterNames.makeString(", ", " <", ">"))
    }

    override fun calculateQuickResult(context: ExpressionContext?): Result = calculateResult(context)

    // do not offer the user any choices
    override fun calculateLookupItems(context: ExpressionContext?) = array<LookupElement>()
}

private fun JetType.contains(inner: JetType): Boolean {
    return this == inner || getArguments().any { inner in it.getType() }
}

private fun DeclarationDescriptor.render(
        typeParameterNameMap: Map<TypeParameterDescriptor, String>,
        fq: Boolean
): String = when {
    this is TypeParameterDescriptor -> typeParameterNameMap[this] ?: getName().asString()
    fq -> DescriptorUtils.getFqName(this).asString()
    else -> getName().asString()
}

private fun JetType.render(typeParameterNameMap: Map<TypeParameterDescriptor, String>, fq: Boolean): String {
    val arguments = getArguments().map { it.getType().render(typeParameterNameMap, fq) }
    val typeString = getConstructor().getDeclarationDescriptor()!!.render(typeParameterNameMap, fq)
    val typeArgumentString = if (arguments.notEmpty) arguments.makeString(", ", "<", ">") else ""
    return "$typeString$typeArgumentString"
}

private fun JetType.renderShort(typeParameterNameMap: Map<TypeParameterDescriptor, String>) = render(typeParameterNameMap, false)
private fun JetType.renderLong(typeParameterNameMap: Map<TypeParameterDescriptor, String>) = render(typeParameterNameMap, true)

private fun getTypeParameterNamesNotInScope(typeParameters: Collection<TypeParameterDescriptor>, scope: JetScope): List<TypeParameterDescriptor> {
    return typeParameters.filter { typeParameter ->
        val classifier = scope.getClassifier(typeParameter.getName())
        classifier == null || classifier != typeParameter
    }
}

private fun JetType.getTypeParameters(): Set<TypeParameterDescriptor> {
    val typeParameters = LinkedHashSet<TypeParameterDescriptor>()
    val arguments = getArguments()
    if (arguments.empty) {
        val descriptor = getConstructor().getDeclarationDescriptor()
        if (descriptor is TypeParameterDescriptor) {
            typeParameters.add(descriptor as TypeParameterDescriptor)
        }
    }
    else {
        arguments.flatMapTo(typeParameters) { projection ->
            projection.getType().getTypeParameters()
        }
    }
    return typeParameters
}

/**
 * Returns the given <code>name</code>, appended with a number if it is one of the <code>existingNames</code> or already exists in
 * <code>scope</code>. For example, given <code>"foo"</code>, returns the next non-conflicting name in the list <code>"foo"</code>,
 * <code>"foo1"</code>, <code>"foo2"</code>, etc.
 */
private fun getNextAvailableName(name: String, existingNames: Collection<String>, scope: JetScope?): String {
    return if (isConflictingName(name, existingNames, scope)) {
        "$name${stream(1) { it + 1 } first { !isConflictingName("$name$it", existingNames, scope) }}"
    }
    else {
        name
    }
}

private fun isConflictingName(name: String, existingNames: Collection<String>, scope: JetScope?): Boolean {
    return name in existingNames || scope?.getClassifier(Name.identifier(name)) != null
}

private fun JetExpression.guessTypes(context: BindingContext): Array<JetType> {

    // if we know the actual type of the expression
    val theType1 = context[BindingContext.EXPRESSION_TYPE, this]
    if (theType1 != null) {
        return array(theType1)
    }

    // expression has an expected type
    val theType2 = context[BindingContext.EXPECTED_EXPRESSION_TYPE, this]
    if (theType2 != null) {
        return array(theType2)
    }

    return when {
        this is JetTypeConstraint -> {
            // expression itself is a type assertion
            val constraint = (this as JetTypeConstraint)
            array(context[BindingContext.TYPE, constraint.getBoundTypeReference()]!!)
        }
        getParent() is JetTypeConstraint -> {
            // expression is on the left side of a type assertion
            val constraint = (getParent() as JetTypeConstraint)
            array(context[BindingContext.TYPE, constraint.getBoundTypeReference()]!!)
        }
        this is JetMultiDeclarationEntry -> {
            // expression is on the lhs of a multi-declaration
            val typeRef = getTypeRef()
            if (typeRef != null) {
                // and has a specified type
                array(context[BindingContext.TYPE, typeRef]!!)
            }
            else {
                // otherwise guess
                guessType(context)
            }
        }
        this is JetParameter -> {
            // expression is a parameter (e.g. declared in a for-loop)
            val typeRef = getTypeReference()
            if (typeRef != null) {
                // and has a specified type
                array(context[BindingContext.TYPE, typeRef]!!)
            }
            else {
                // otherwise guess
                guessType(context)
            }
        }
        getParent() is JetVariableDeclaration -> {
            // the expression is the RHS of a variable assignment with a specified type
            val variable = getParent() as JetVariableDeclaration
            val typeRef = variable.getTypeRef()
            if (typeRef != null) {
                // and has a specified type
                array(context[BindingContext.TYPE, typeRef]!!)
            }
            else {
                // otherwise guess, based on LHS
                variable.guessType(context)
            }
        }
        else -> array() // can't infer anything
    }
}

private fun JetNamedDeclaration.guessType(context: BindingContext): Array<JetType> {
    val scope = getContainingFile()!!.getUseScope()
    val expectedTypes = SearchUtils.findAllReferences(this, scope)!!.stream().map { ref ->
        if (ref is JetSimpleNameReference) {
            context[BindingContext.EXPECTED_EXPRESSION_TYPE, ref.expression]
        }
        else {
            null
        }
    }.filterNotNullTo(HashSet<JetType>())

    if (expectedTypes.isEmpty() || expectedTypes.any { expectedType -> ErrorUtils.containsErrorType(expectedType) }) {
        return array<JetType>()
    }
    val theType = TypeUtils.intersect(JetTypeChecker.DEFAULT, expectedTypes)
    if (theType != null) {
        return array<JetType>(theType)
    }
    else {
        // intersection doesn't exist; let user make an imperfect choice
        return expectedTypes.copyToArray()
    }
}

/**
 * Encapsulates a single type substitution of a <code>JetType</code> by another <code>JetType</code>.
 */
private class JetTypeSubstitution(public val forType: JetType, public val byType: JetType)

private fun JetType.substitute(substitution: JetTypeSubstitution, variance: Variance): JetType {
    if (when (variance) {
        Variance.INVARIANT      -> this == substitution.forType
        Variance.IN_VARIANCE    -> JetTypeChecker.DEFAULT.isSubtypeOf(this, substitution.forType)
        Variance.OUT_VARIANCE   -> JetTypeChecker.DEFAULT.isSubtypeOf(substitution.forType, this)
    }) {
        return substitution.byType
    }
    else {
        val newArguments = getArguments().zip(getConstructor().getParameters()).map { pair ->
            val (projection, typeParameter) = pair
            TypeProjectionImpl(Variance.INVARIANT, projection.getType().substitute(substitution, typeParameter.getVariance()))
        }
        return JetTypeImpl(getAnnotations(), getConstructor(), isNullable(), newArguments, getMemberScope())
    }
}

public class CreateFunctionFromUsageFix internal (
        element: PsiElement,
        private val ownerType: TypeOrExpressionThereof,
        private val functionName: String,
        private val returnType: TypeOrExpressionThereof,
        private val parameters: List<Parameter> = Collections.emptyList()
) : CreateFromUsageFixBase(element) {
    private var isUnit: Boolean = false
    private var isExtension: Boolean = false
    private var currentFile: JetFile by Delegates.notNull()
    private var containingFile: JetFile by Delegates.notNull()
    private var currentFileEditor: Editor by Delegates.notNull()
    private var containingFileEditor: Editor by Delegates.notNull()
    private var currentFileContext: BindingContext by Delegates.notNull()
    private var currentFileModule: ModuleDescriptor by Delegates.notNull()
    private var ownerClass: JetClass by Delegates.notNull()
    private var ownerClassDescriptor: ClassDescriptor by Delegates.notNull()
    private var selectedReceiverType: TypeCandidate by Delegates.notNull()
    private var typeParameterNameMap: Map<TypeParameterDescriptor, String> by Delegates.notNull()

    override fun getText(): String {
        return JetBundle.message("create.function.from.usage", functionName)
    }

    override fun invoke(project: Project, editor: Editor?, file: JetFile?) {
        currentFile = file!!
        currentFileEditor = editor!!

        val exhaust = currentFile.getAnalysisResults()
        currentFileContext = exhaust.getBindingContext()
        currentFileModule = exhaust.getModuleDescriptor()

        ownerType.computeTypeCandidates(currentFileContext)
        val ownerTypeCandidates = ownerType.typeCandidates!!
        assert(!ownerTypeCandidates.empty)

        if (ownerTypeCandidates.size == 1 || ApplicationManager.getApplication()!!.isUnitTestMode()) {
            selectedReceiverType = ownerTypeCandidates.first!!
            addFunctionToSelectedOwner()
        }
        else {
            // class selection
            val list = JBList(ownerTypeCandidates.map { ClassCandidate(it, currentFile) })
            val renderer = QuickFixUtil.ClassCandidateListCellRenderer()
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            list.setCellRenderer(renderer)
            val builder = PopupChooserBuilder(list)
            renderer.installSpeedSearch(builder)

            builder.setTitle(JetBundle.message("choose.target.class.or.trait.title"))
                    .setItemChoosenCallback {
                        val selectedCandidate = list.getSelectedValue() as ClassCandidate?
                        if (selectedCandidate != null) {
                            selectedReceiverType = selectedCandidate.typeCandidate
                            CommandProcessor.getInstance().executeCommand(project, { addFunctionToSelectedOwner() }, getText(), null)
                        }
                    }
                    .createPopup()
                    .showInBestPositionFor(currentFileEditor)
        }
    }

    private fun addFunctionToSelectedOwner() {
        // gather relevant information
        ownerClassDescriptor = DescriptorUtils.getClassDescriptorForType(selectedReceiverType.theType)
        val receiverType = ownerClassDescriptor.getDefaultType()
        val classDeclaration = DescriptorToSourceUtils.classDescriptorToDeclaration(ownerClassDescriptor)
        if (classDeclaration is JetClass) {
            ownerClass = classDeclaration
            isExtension = !ownerClass.isWritable()
        }
        else {
            isExtension = true
        }
        isUnit = returnType.isType() && KotlinBuiltIns.getInstance().isUnit(returnType.theType!!)

        val scope = if (isExtension) {
            currentFileModule.getPackage(currentFile.getPackageFqName())!!.getMemberScope()
        }
        else {
            (ownerClassDescriptor as ClassDescriptorWithResolutionScopes).getScopeForMemberDeclarationResolution()
        }

        // figure out type substitutions for type parameters
        val classTypeParameters = receiverType.getArguments()
        val ownerTypeArguments = selectedReceiverType.theType.getArguments()
        assert(ownerTypeArguments.size == classTypeParameters.size)
        val substitutions = ownerTypeArguments.zip(classTypeParameters).map {
            JetTypeSubstitution(it.first.getType(), it.second.getType())
        }.copyToArray()
        parameters.forEach { parameter ->
            parameter.theType.computeTypeCandidates(currentFileContext, substitutions, scope)
        }
        if (!isUnit) {
            returnType.computeTypeCandidates(currentFileContext, substitutions, scope)
        }

        // now that we have done substitutions, we can throw it away
        selectedReceiverType = TypeCandidate(receiverType, scope)

        // figure out type parameter renames to avoid conflicts
        typeParameterNameMap = getTypeParameterRenames(scope)
        parameters.forEach { parameter ->
            parameter.theType.renderTypeCandidates(typeParameterNameMap)
        }
        if (!isUnit) {
            returnType.renderTypeCandidates(typeParameterNameMap)
        }
        selectedReceiverType.render(typeParameterNameMap)

        ApplicationManager.getApplication()!!.runWriteAction {
            val func = createFunctionSkeleton()
            buildAndRunTemplate(func)
        }
    }

    private fun createFunctionSkeleton(): JetNamedFunction {
        val parametersString = parameters.indices.map { i -> "p$i: Any" }.makeString(", ")
        val returnTypeString = if (isUnit) "" else ": Any"
        val psiFactory = JetPsiFactory(currentFile)
        if (isExtension) {
            // create as extension function
            val ownerTypeString = selectedReceiverType.renderedType!!
            val func = psiFactory.createFunction("fun $ownerTypeString.$functionName($parametersString)$returnTypeString { }")
            containingFile = currentFile
            containingFileEditor = currentFileEditor
            return currentFile.add(func) as JetNamedFunction
        }
        else {
            // create as regular function
            val func = psiFactory.createFunction("fun $functionName($parametersString)$returnTypeString { }")
            containingFile = ownerClass.getContainingJetFile()

            NavigationUtil.activateFileWithPsiElement(containingFile)
            containingFileEditor = FileEditorManager.getInstance(currentFile.getProject())!!.getSelectedTextEditor()!!

            var classBody = ownerClass.getBody()
            if (classBody == null) {
                classBody = ownerClass.add(psiFactory.createEmptyClassBody()) as JetClassBody
                ownerClass.addBefore(psiFactory.createWhiteSpace(), classBody)
            }
            val rBrace = classBody!!.getRBrace()
            //TODO: Assert rbrace not null? It can be if the class isn't closed.
            return classBody!!.addBefore(func, rBrace) as JetNamedFunction
        }
    }

    private fun buildAndRunTemplate(func: JetNamedFunction) {
        val project = func.getProject()
        val parameterList = func.getValueParameterList()!!

        // build templates
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(containingFileEditor.getDocument())

        val caretModel = containingFileEditor.getCaretModel()
        caretModel.moveToOffset(containingFile.getNode()!!.getStartOffset())

        val builder = TemplateBuilderImpl(containingFile)
        val returnTypeExpression = if (isUnit) null else setupReturnTypeTemplate(builder, func)
        val parameterTypeExpressions = setupParameterTypeTemplates(builder, parameterList)

        // add a segment for the parameter list
        // Note: because TemplateBuilderImpl does not have a replaceElement overload that takes in both a TextRange and alwaysStopAt, we
        // need to create the segment first and then hack the Expression into the template later. We use this template to update the type
        // parameter list as the user makes selections in the parameter types, and we need alwaysStopAt to be false so the user can't tab to
        // it.
        val expression = setupTypeParameterListTemplate(builder, func)

        // the template built by TemplateBuilderImpl is ordered by element position, but we want types to be first, so hack it
        val templateImpl = builder.buildInlineTemplate() as TemplateImpl
        val variables = templateImpl.getVariables()!!
        for (i in 0..(parameters.size - 1)) {
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
                val newFunc = PsiTreeUtil.findElementOfClassAtOffset(containingFile, offset, javaClass<JetNamedFunction>(), false)!!
                val typeRefsToShorten = ArrayList<JetTypeReference>()

                ApplicationManager.getApplication()!!.runWriteAction {
                    // file templates
                    setupFunctionBody(newFunc)

                    // change short type names to fully qualified ones (to be shortened below)
                    setupTypeReferencesForShortening(newFunc, typeRefsToShorten, parameterTypeExpressions, returnTypeExpression)
                    ShortenReferences.process(typeRefsToShorten)
                }
            }
        })
    }

    private fun getTypeParameterRenames(scope: JetScope): Map<TypeParameterDescriptor, String> {
        val allTypeParametersNotInScope = LinkedHashSet<TypeParameterDescriptor>()

        allTypeParametersNotInScope.addAll(selectedReceiverType.typeParameters.toList())

        parameters.stream()
                .flatMap { it.theType.typeCandidates!!.stream() }
                .flatMap { it.typeParameters.stream() }
                .toCollection(allTypeParametersNotInScope)

        if (!isUnit) {
            returnType.typeCandidates!!.stream().flatMapTo(allTypeParametersNotInScope) { it.typeParameters.stream() }
        }

        val typeParameterNames = ArrayList<String>()
        allTypeParametersNotInScope.mapTo(typeParameterNames) { typeParameter ->
            getNextAvailableName(typeParameter.getName().asString(), typeParameterNames, scope)
        }

        val typeParameterNameMap = HashMap<TypeParameterDescriptor, String>()
        for ((key, value) in allTypeParametersNotInScope.zip(typeParameterNames)) {
            typeParameterNameMap[key] = value
        }

        return typeParameterNameMap
    }

    private fun setupTypeReferencesForShortening(
            func: JetNamedFunction,
            typeRefsToShorten: MutableList<JetTypeReference>, parameterTypeExpressions: List<TypeExpression>,
            returnTypeExpression: TypeExpression?) {
        if (isExtension) {
            val receiverTypeRef = JetPsiFactory(func).createType(selectedReceiverType.theType.renderLong(typeParameterNameMap))
            replaceWithLongerName(receiverTypeRef, selectedReceiverType.theType)

            val funcReceiverTypeRef = func.getReceiverTypeRef()
            if (funcReceiverTypeRef != null) {
                typeRefsToShorten.add(funcReceiverTypeRef)
            }
        }

        if (!isUnit) {
            returnTypeExpression!!
            val returnTypeRef = func.getReturnTypeRef()
            if (returnTypeRef != null) {
                val returnType = returnTypeExpression.getTypeFromSelection(returnTypeRef.getText() ?: throw AssertionError("Expression for function return type shouldn't be empty: function = ${func.getText()}"))
                if (returnType != null) {
                    // user selected a given type
                    replaceWithLongerName(returnTypeRef, returnType)
                    typeRefsToShorten.add(func.getReturnTypeRef()!!)
                }
            }
        }

        val valueParameters = func.getValueParameters()
        val parameterIndicesToShorten = ArrayList<Int>()
        assert(valueParameters.size == parameterTypeExpressions.size)
        for ((i, parameter) in valueParameters.stream().withIndices()) {
            val parameterTypeRef = parameter.getTypeReference()
            if (parameterTypeRef != null) {
                val parameterType = parameterTypeExpressions[i].getTypeFromSelection(parameterTypeRef.getText() ?: throw AssertionError("Expression for function parameter type shouldn't be empty: function = ${func.getText()}"))
                if (parameterType != null) {
                    replaceWithLongerName(parameterTypeRef, parameterType)
                    parameterIndicesToShorten.add(i)
                }
            }
        }

        val expandedValueParameters = func.getValueParameters()
        parameterIndicesToShorten.stream().map { expandedValueParameters[it].getTypeReference() }.filterNotNullTo(typeRefsToShorten)
    }


    private fun setupFunctionBody(func: JetNamedFunction) {
        val fileTemplate = FileTemplateManager.getInstance()!!.getCodeTemplate(TEMPLATE_FROM_USAGE_FUNCTION_BODY)
        val properties = Properties()
        properties.setProperty(FileTemplate.ATTRIBUTE_RETURN_TYPE, if (isUnit) "Unit" else func.getReturnTypeRef()!!.getText())
        properties.setProperty(FileTemplate.ATTRIBUTE_CLASS_NAME, DescriptorUtils.getFqName(ownerClassDescriptor).asString())
        properties.setProperty(FileTemplate.ATTRIBUTE_SIMPLE_CLASS_NAME, ownerClassDescriptor.getName().asString())
        properties.setProperty(ATTRIBUTE_FUNCTION_NAME, functionName)

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

    private fun setupReturnTypeTemplate(builder: TemplateBuilder, func: JetNamedFunction): TypeExpression {
        val returnTypeRef = func.getReturnTypeRef()!!
        val returnTypeExpression = TypeExpression(returnType)
        builder.replaceElement(returnTypeRef, returnTypeExpression)
        return returnTypeExpression
    }

    private fun setupTypeParameterListTemplate(builder: TemplateBuilderImpl, func: JetNamedFunction): TypeParameterListExpression {
        val typeParameterMap = HashMap<String, Array<String>>()
        val receiverTypeParameterNames = selectedReceiverType.typeParameterNames

        parameters.stream().flatMap { it.theType.typeCandidates!!.stream() }.forEach {
            typeParameterMap[it.renderedType!!] = it.typeParameterNames!!
        }

        if (func.getReturnTypeRef() != null) {
            returnType.typeCandidates!!.forEach {
                typeParameterMap[it.renderedType!!] = it.typeParameterNames!!
            }
        }
        // ((3, 3) is after "fun")
        builder.replaceElement(func, TextRange.create(3, 3), TYPE_PARAMETER_LIST_VARIABLE_NAME, null, false)
        return TypeParameterListExpression(receiverTypeParameterNames!!, typeParameterMap)
    }

    private fun setupParameterTypeTemplates(builder: TemplateBuilder, parameterList: JetParameterList): List<TypeExpression> {
        val jetParameters = parameterList.getParameters()
        assert(jetParameters.size == parameters.size)
        val dummyValidator = JetNameValidator.getEmptyValidator(parameterList.getProject())

        val typeParameters = ArrayList<TypeExpression>()
        for ((parameter, jetParameter) in parameters.zip(jetParameters)) {
            val parameterTypeExpression = TypeExpression(parameter.theType)
            val parameterTypeRef = jetParameter.getTypeReference()!!
            builder.replaceElement(parameterTypeRef, parameterTypeExpression)

            // add parameter name to the template
            val possibleNamesFromExpression = parameter.theType.possibleNamesFromExpression
            val preferredName = parameter.preferredName
            val possibleNames = if (preferredName != null) {
                array(preferredName, *possibleNamesFromExpression)
            }
            else {
                possibleNamesFromExpression
            }

            // figure out suggested names for each type option
            val parameterTypeToNamesMap = HashMap<String, Array<String>>()
            parameter.theType.typeCandidates!!.forEach { typeCandidate ->
                val suggestedNames = JetNameSuggester.suggestNamesForType(typeCandidate.theType, dummyValidator)
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

    class object {
        public fun createCreateGetFunctionFromUsageFactory(): JetSingleIntentionActionFactory {
            return object : JetSingleIntentionActionFactory() {
                override fun createAction(diagnostic: Diagnostic?): IntentionAction? {
                    val accessExpr = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetArrayAccessExpression>()) ?: return null
                    val arrayExpr = accessExpr.getArrayExpression() ?: return null
                    val arrayType = TypeOrExpressionThereof(arrayExpr, Variance.IN_VARIANCE)

                    val parameters = accessExpr.getIndexExpressions().map {
                        Parameter(TypeOrExpressionThereof(it, Variance.IN_VARIANCE))
                    }

                    val returnType = TypeOrExpressionThereof(accessExpr, Variance.OUT_VARIANCE)
                    return CreateFunctionFromUsageFix(accessExpr, arrayType, "get", returnType, parameters)
                }
            }
        }

        public fun createCreateSetFunctionFromUsageFactory(): JetSingleIntentionActionFactory {
            return object : JetSingleIntentionActionFactory() {
                override fun createAction(diagnostic: Diagnostic?): IntentionAction? {
                    val accessExpr = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetArrayAccessExpression>()) ?: return null
                    val arrayExpr = accessExpr.getArrayExpression() ?: return null
                    val arrayType = TypeOrExpressionThereof(arrayExpr, Variance.IN_VARIANCE)

                    val parameters = accessExpr.getIndexExpressions().mapTo(ArrayList<Parameter>()) {
                        Parameter(TypeOrExpressionThereof(it, Variance.IN_VARIANCE))
                    }

                    val assignmentExpr = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetBinaryExpression>()) ?: return null
                    val rhs = assignmentExpr.getRight() ?: return null
                    val valType = TypeOrExpressionThereof(rhs, Variance.IN_VARIANCE)
                    parameters.add(Parameter(valType, "value"))

                    val returnType = TypeOrExpressionThereof(KotlinBuiltIns.getInstance().getUnitType(), Variance.OUT_VARIANCE)
                    return CreateFunctionFromUsageFix(accessExpr, arrayType, "set", returnType, parameters)
                }
            }
        }

        public fun createCreateHasNextFunctionFromUsageFactory(): JetSingleIntentionActionFactory {
            return object : JetSingleIntentionActionFactory() {
                override fun createAction(diagnostic: Diagnostic?): IntentionAction? {
                    val diagnosticWithParameters = DiagnosticFactory.cast(diagnostic!!, Errors.HAS_NEXT_MISSING, Errors.HAS_NEXT_FUNCTION_NONE_APPLICABLE)
                    val ownerType = TypeOrExpressionThereof(diagnosticWithParameters.getA(), Variance.IN_VARIANCE)

                    val forExpr = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetForExpression>()) ?: return null
                    val returnType = TypeOrExpressionThereof(KotlinBuiltIns.getInstance().getBooleanType(), Variance.OUT_VARIANCE)
                    return CreateFunctionFromUsageFix(forExpr, ownerType, "hasNext", returnType)
                }
            }
        }

        public fun createCreateNextFunctionFromUsageFactory(): JetSingleIntentionActionFactory {
            return object : JetSingleIntentionActionFactory() {
                override fun createAction(diagnostic: Diagnostic?): IntentionAction? {
                    val diagnosticWithParameters = DiagnosticFactory.cast(diagnostic!!, Errors.NEXT_MISSING, Errors.NEXT_NONE_APPLICABLE)
                    val ownerType = TypeOrExpressionThereof(diagnosticWithParameters.getA(), Variance.IN_VARIANCE)

                    val forExpr = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetForExpression>()) ?: return null
                    val variableExpr: JetExpression = ((forExpr.getLoopParameter() ?: forExpr.getMultiParameter()) ?: return null) as JetExpression
                    val returnType = TypeOrExpressionThereof(variableExpr, Variance.OUT_VARIANCE)
                    return CreateFunctionFromUsageFix(forExpr, ownerType, "next", returnType)
                }
            }
        }

        public fun createCreateIteratorFunctionFromUsageFactory(): JetSingleIntentionActionFactory {
            return object : JetSingleIntentionActionFactory() {
                override fun createAction(diagnostic: Diagnostic?): IntentionAction? {
                    val file = diagnostic!!.getPsiFile() as? JetFile ?: return null
                    val forExpr = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetForExpression>()) ?: return null
                    val iterableExpr = forExpr.getLoopRange() ?: return null
                    val variableExpr: JetExpression = ((forExpr.getLoopParameter() ?: forExpr.getMultiParameter()) ?: return null) as JetExpression
                    val iterableType = TypeOrExpressionThereof(iterableExpr, Variance.IN_VARIANCE)
                    val returnJetType = KotlinBuiltIns.getInstance().getIterator().getDefaultType()

                    val context = file.getBindingContext()
                    val returnJetTypeParameterTypes = variableExpr.guessTypes(context)
                    if (returnJetTypeParameterTypes.size != 1) return null

                    val returnJetTypeParameterType = TypeProjectionImpl(returnJetTypeParameterTypes[0])
                    val returnJetTypeArguments = Collections.singletonList(returnJetTypeParameterType)
                    val newReturnJetType = JetTypeImpl(returnJetType.getAnnotations(), returnJetType.getConstructor(), returnJetType.isNullable(), returnJetTypeArguments, returnJetType.getMemberScope())
                    val returnType = TypeOrExpressionThereof(newReturnJetType, Variance.OUT_VARIANCE)
                    return CreateFunctionFromUsageFix(forExpr, iterableType, "iterator", returnType)
                }
            }
        }

        public fun createCreateComponentFunctionFromUsageFactory(): JetSingleIntentionActionFactory {
            return object : JetSingleIntentionActionFactory() {
                override fun createAction(diagnostic: Diagnostic?): IntentionAction? {
                    val diagnosticWithParameters = Errors.COMPONENT_FUNCTION_MISSING.cast(diagnostic!!)
                    val name = diagnosticWithParameters.getA()
                    val componentNumberMatcher = COMPONENT_FUNCTION_PATTERN.matcher(name.getIdentifier())
                    if (!componentNumberMatcher.matches()) return null
                    val componentNumberString = componentNumberMatcher.group(1)!!
                    val componentNumber = Integer.decode(componentNumberString)!! - 1

                    var multiDeclaration = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetMultiDeclaration>())
                    val ownerType = if (multiDeclaration == null) {
                        val forExpr = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetForExpression>())!!
                        multiDeclaration = forExpr.getMultiParameter()!!
                        TypeOrExpressionThereof(diagnosticWithParameters.getB(), Variance.IN_VARIANCE)
                    }
                    else {
                        val rhs = multiDeclaration!!.getInitializer() ?: return null
                        TypeOrExpressionThereof(rhs, Variance.IN_VARIANCE)
                    }
                    val entries = multiDeclaration!!.getEntries()

                    val entry = entries[componentNumber]
                    val returnType = TypeOrExpressionThereof(entry, Variance.OUT_VARIANCE)

                    return CreateFunctionFromUsageFix(multiDeclaration!!, ownerType, name.getIdentifier(), returnType)
                }
            }
        }
    }
}
