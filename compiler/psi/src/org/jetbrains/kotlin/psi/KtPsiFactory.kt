/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.LocalTimeCounter
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.psiUtil.isIdentifier
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.utils.checkWithAttachment

@JvmOverloads
@JvmName("KtPsiFactory")
@Suppress("unused")
@Deprecated(
    "Use 'KtPsiFactory' constructor instead",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("KtPsiFactory(project!!, markGenerated)", "org.jetbrains.kotlin.psi.KtPsiFactory")
)
fun KtPsiFactory(project: Project?, markGenerated: Boolean = true): KtPsiFactory = KtPsiFactory(project!!, markGenerated)

@JvmOverloads
@JvmName("KtPsiFactory")
@Suppress("unused")
@Deprecated(
    "Use 'KtPsiFactory' constructor instead",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("KtPsiFactory(elementForProject.project, markGenerated)", "org.jetbrains.kotlin.psi.KtPsiFactory")
)
fun KtPsiFactory(elementForProject: PsiElement, markGenerated: Boolean = true): KtPsiFactory =
    KtPsiFactory(elementForProject.project, markGenerated)

private const val DO_NOT_ANALYZE_NOTIFICATION = "This file was created by KtPsiFactory and should not be analyzed\n" +
        "Use createAnalyzableFile to create file that can be analyzed\n"

var KtFile.doNotAnalyze: String? by UserDataProperty(Key.create("DO_NOT_ANALYZE"))
var KtFile.analysisContext: PsiElement? by UserDataProperty(Key.create("ANALYSIS_CONTEXT"))


/**
 * @param markGenerated This needs to be set to true if the `KtPsiFactory` is going to be used for creating elements that are going
 * to be inserted in the user source code (this ensures that the elements will be formatted correctly). In other cases, `markGenerated`
 * should be false, which saves time and memory.
 */
class KtPsiFactory private constructor(
    private val project: Project,
    private val markGenerated: Boolean,
    private val context: PsiElement?,
    private val eventSystemEnabled: Boolean,
) {
    companion object {
        @JvmStatic
        @JvmOverloads
        fun contextual(context: PsiElement, markGenerated: Boolean = true): KtPsiFactory {
            return KtPsiFactory(context.project, markGenerated, context, eventSystemEnabled = false)
        }
    }

    @JvmOverloads
    constructor(project: Project, markGenerated: Boolean = true) :
            this(project, markGenerated, context = null, eventSystemEnabled = false)

    constructor(project: Project, markGenerated: Boolean = true, eventSystemEnabled: Boolean) :
            this(project, markGenerated, context = null, eventSystemEnabled = eventSystemEnabled)


    @JvmOverloads
    @Deprecated("Use 'KtPsiFactory(project, markGenerated)' or 'KtPsiFactory.contextual(context, markGenerated)' instead")
    constructor(element: KtElement, markGenerated: Boolean = true) : this(element.project, markGenerated, context = null, eventSystemEnabled = false)

    fun createValKeyword(): PsiElement {
        val property = createProperty("val x = 1")
        return property.valOrVarKeyword
    }

    fun createVarKeyword(): PsiElement {
        val property = createProperty("var x = 1")
        return property.valOrVarKeyword
    }

    private fun doCreateExpression(@NonNls text: String): KtExpression? {
        //NOTE: '\n' below is important - some strange code indenting problems appear without it
        return createProperty("val x =\n$text").initializer
    }

    fun createExpression(@NonNls text: String): KtExpression {
        val expression = doCreateExpression(text) ?: error("Failed to create expression from text: '$text'")
        assert(expression.text == text) {
            "Failed to create expression from text: '$text', resulting expression's text was: '${expression.text}'"
        }
        return expression
    }

    fun createExpressionIfPossible(@NonNls text: String): KtExpression? {
        val expression = try {
            doCreateExpression(text) ?: return null
        } catch (ignored: Throwable) {
            return null
        }
        return if (expression.text == text) expression else null
    }

    fun createThisExpression() =
        (createExpression("this.x") as KtQualifiedExpression).receiverExpression as KtThisExpression

    fun createThisExpression(@NonNls qualifier: String) =
        (createExpression("this@$qualifier.x") as KtQualifiedExpression).receiverExpression as KtThisExpression

    fun createCallArguments(@NonNls text: String): KtValueArgumentList {
        val property = createProperty("val x = foo $text")
        return (property.initializer as KtCallExpression).valueArgumentList!!
    }

    fun createTypeArguments(@NonNls text: String): KtTypeArgumentList {
        val property = createProperty("val x = foo$text()")
        return (property.initializer as KtCallExpression).typeArgumentList!!
    }

    fun createTypeArgument(@NonNls text: String) = createTypeArguments("<$text>").arguments.first()

    fun createType(@NonNls type: String): KtTypeReference {
        val typeReference = createTypeIfPossible(type)
        if (typeReference == null || typeReference.text != type) {
            throw IllegalArgumentException("Incorrect type: $type")
        }
        return typeReference
    }

    fun createType(typeElement: KtTypeElement) = createType("X").apply { this.typeElement!!.replace(typeElement) }

    fun createTypeIfPossible(@NonNls type: String): KtTypeReference? {
        val typeReference = createProperty("val x : $type").typeReference
        return if (typeReference?.text == type) typeReference else null
    }

    fun createFunctionTypeReceiver(typeReference: KtTypeReference): KtFunctionTypeReceiver {
        return (createType("A.() -> B").typeElement as KtFunctionType).receiver!!.apply { this.typeReference.replace(typeReference) }
    }

    fun createFunctionTypeParameter(typeReference: KtTypeReference): KtParameter {
        return (createType("(A) -> B").typeElement as KtFunctionType).parameters.first()
            .apply { this.typeReference!!.replace(typeReference) }
    }

    fun createTypeAlias(@NonNls name: String, typeParameters: List<String>, typeElement: KtTypeElement): KtTypeAlias {
        return createTypeAlias(name, typeParameters, "X").apply { getTypeReference()!!.replace(createType(typeElement)) }
    }

    fun createTypeAlias(@NonNls name: String, typeParameters: List<String>, @NonNls body: String): KtTypeAlias {
        val typeParametersText = if (typeParameters.isNotEmpty()) typeParameters.joinToString(prefix = "<", postfix = ">") else ""
        return createDeclaration("typealias $name$typeParametersText = $body")
    }

    fun createStar(): PsiElement {
        return createType("List<*>").findElementAt(5)!!
    }

    fun createComma(): PsiElement {
        return createType("T<X, Y>").findElementAt(3)!!
    }

    fun createDot(): PsiElement {
        return createType("T.(X)").findElementAt(1)!!
    }

    fun createColon(): PsiElement {
        return createProperty("val x: Int").findElementAt(5)!!
    }

    fun createEQ(): PsiElement {
        return createFunction("fun foo() = foo").equalsToken!!
    }

    fun createSemicolon(): PsiElement {
        return createProperty("val x: Int;").findElementAt(10)!!
    }

    //the pair contains the first and the last elements of a range
    fun createWhitespaceAndArrow(): Pair<PsiElement, PsiElement> {
        val functionType = createType("() -> Int").typeElement as KtFunctionType
        return Pair(functionType.findElementAt(2)!!, functionType.findElementAt(3)!!)
    }

    fun createWhiteSpace(): PsiElement {
        return createWhiteSpace(" ")
    }

    fun createWhiteSpace(@NonNls text: String): PsiElement {
        return createProperty("val${text}x: Int").findElementAt(3)!!
    }

    // Remove when all Java usages are rewritten to Kotlin
    fun createNewLine(): PsiElement {
        return createWhiteSpace("\n ")
    }

    fun createNewLine(lineBreaks: Int): PsiElement {
        return createWhiteSpace("\n".repeat(lineBreaks))
    }

    fun createClass(@NonNls text: String): KtClass {
        return createDeclaration(text)
    }

    fun createObject(@NonNls text: String): KtObjectDeclaration {
        return createDeclaration(text)
    }

    fun createCompanionObject(): KtObjectDeclaration {
        return createCompanionObject("companion object {\n}")
    }

    fun createCompanionObject(@NonNls text: String): KtObjectDeclaration {
        return createClass("class A {\n $text\n}").companionObjects.first()
    }

    fun createFileAnnotation(@NonNls annotationText: String): KtAnnotationEntry {
        return createFileAnnotationListWithAnnotation(annotationText).annotationEntries.first()
    }

    fun createFileAnnotationListWithAnnotation(@NonNls annotationText: String): KtFileAnnotationList {
        return createFile("@file:$annotationText").fileAnnotationList!!
    }

    fun createFile(@NonNls text: String): KtFile {
        return createFile("dummy.kt", text)
    }

    private fun doCreateFile(@NonNls fileName: String, @NonNls text: String): KtFile {
        return PsiFileFactory.getInstance(project).createFileFromText(
            fileName,
            KotlinFileType.INSTANCE,
            text,
            LocalTimeCounter.currentTime(),
            eventSystemEnabled,
            markGenerated
        ) as KtFile
    }

    fun createFile(@NonNls fileName: String, @NonNls text: String): KtFile {
        val file = doCreateFile(fileName, text)

        val analysisContext = this@KtPsiFactory.context
        if (analysisContext != null) {
            file.analysisContext = analysisContext
        } else {
            file.doNotAnalyze = DO_NOT_ANALYZE_NOTIFICATION
        }

        return file
    }

    @Deprecated("Call 'createFile()' on a contextual 'KtPsiFactory' instead")
    fun createAnalyzableFile(@NonNls fileName: String, @NonNls text: String, contextToAnalyzeIn: PsiElement): KtFile {
        val file = doCreateFile(fileName, text)
        file.analysisContext = contextToAnalyzeIn
        return file
    }

    @Deprecated("Call 'createPhysicalFile() on a contextual 'KtPsiFactory' instead")
    fun createFileWithLightClassSupport(@NonNls fileName: String, @NonNls text: String, contextToAnalyzeIn: PsiElement): KtFile {
        val file = createPhysicalFile(fileName, text)
        file.analysisContext = contextToAnalyzeIn
        return file
    }

    fun createPhysicalFile(@NonNls fileName: String, @NonNls text: String): KtFile {
        val time = LocalTimeCounter.currentTime()
        val file = PsiFileFactory.getInstance(project).createFileFromText(fileName, KotlinFileType.INSTANCE, text, time, true) as KtFile
        file.analysisContext = this@KtPsiFactory.context
        return file
    }

    fun createProperty(
        @NonNls modifiers: String?,
        @NonNls name: String,
        @NonNls type: String?,
        isVar: Boolean,
        @NonNls initializer: String?
    ): KtProperty {
        val text = modifiers.let { "$it " } +
                (if (isVar) " var " else " val ") + name +
                (if (type != null) ":$type" else "") + (if (initializer == null) "" else " = $initializer")
        return createProperty(text)
    }

    fun createProperty(@NonNls name: String, @NonNls type: String?, isVar: Boolean, @NonNls initializer: String?): KtProperty {
        return createProperty(null, name, type, isVar, initializer)
    }

    fun createProperty(@NonNls name: String, @NonNls type: String?, isVar: Boolean): KtProperty {
        return createProperty(name, type, isVar, null)
    }

    fun createProperty(@NonNls text: String): KtProperty {
        return createDeclaration(text)
    }

    fun createPropertyGetter(expression: KtExpression): KtPropertyAccessor {
        val property = if (expression is KtBlockExpression)
            createProperty("val x get() {\nreturn 1\n}")
        else
            createProperty("val x get() = 1")
        val getter = property.getter!!
        val bodyExpression = getter.bodyExpression!!

        bodyExpression.replace(expression)
        return getter
    }

    fun createPropertySetter(expression: KtExpression): KtPropertyAccessor {
        val property = if (expression is KtBlockExpression)
            createProperty("val x get() = 1\nset(value) {\n field = value\n }")
        else
            createProperty("val x get() = 1\nset(value) = TODO()")
        val setter = property.setter!!
        val bodyExpression = setter.bodyExpression!!

        bodyExpression.replace(expression)
        return setter
    }

    fun createPropertyDelegate(expression: KtExpression): KtPropertyDelegate {
        val property = createProperty("val x by lazy { 1 }")
        val delegate = property.delegate!!
        val delegateExpression = delegate.expression!!
        delegateExpression.replace(expression)
        return delegate
    }

    fun createDestructuringDeclaration(@NonNls text: String): KtDestructuringDeclaration {
        return createFunction("fun foo() {$text}").bodyBlockExpression!!.statements.first() as KtDestructuringDeclaration
    }

    fun createDestructuringParameter(@NonNls text: String): KtParameter {
        val dummyFun = createFunction("fun foo() = { $text -> }")
        return (dummyFun.bodyExpression as KtLambdaExpression).functionLiteral.valueParameters.first()
    }

    fun <TDeclaration : KtDeclaration> createDeclaration(@NonNls text: String): TDeclaration {
        val file = createFile(text)
        val declarations = file.declarations
        checkWithAttachment(declarations.size == 1, { "unexpected ${declarations.size} declarations" }) {
            it.withAttachment("text.kt", text)
            for (d in declarations.withIndex()) {
                it.withPsiAttachment("declaration${d.index}.kt", d.value)
            }
        }
        @Suppress("UNCHECKED_CAST")
        return declarations.first() as TDeclaration
    }

    fun createNameIdentifier(@NonNls name: String) = createNameIdentifierIfPossible(name)!!

    fun createNameIdentifierIfPossible(@NonNls name: String) = createProperty(name, null, false).nameIdentifier

    fun createSimpleName(@NonNls name: String): KtSimpleNameExpression {
        return createProperty(name, null, false, name).initializer as KtSimpleNameExpression
    }

    fun createOperationName(@NonNls name: String): KtSimpleNameExpression {
        return (createExpression("0 $name 0") as KtBinaryExpression).operationReference
    }

    fun createIdentifier(@NonNls name: String): PsiElement {
        return createSimpleName(name).getIdentifier()!!
    }

    fun createFunction(@NonNls funDecl: String): KtNamedFunction {
        return createDeclaration(funDecl)
    }

    fun createCallableReferenceExpression(@NonNls text: String) = createExpression(text) as? KtCallableReferenceExpression

    fun createSecondaryConstructor(@NonNls decl: String): KtSecondaryConstructor {
        return createClass("class Foo {\n $decl \n}").secondaryConstructors.first()
    }

    fun createModifierList(modifier: KtModifierKeywordToken): KtModifierList {
        return createModifierList(modifier.value)
    }

    fun createModifierList(@NonNls text: String): KtModifierList {
        return createClass("$text interface x").modifierList!!
    }

    fun createEmptyModifierList() = createModifierList(KtTokens.PRIVATE_KEYWORD).apply { firstChild.delete() }

    fun createModifier(modifier: KtModifierKeywordToken): PsiElement {
        return createModifierList(modifier.value).getModifier(modifier)!!
    }

    fun createAnnotationEntry(@NonNls text: String): KtAnnotationEntry {
        val modifierList = createProperty(text + " val x").modifierList
        return modifierList!!.annotationEntries.first()
    }

    fun createEmptyBody(): KtBlockExpression {
        return createFunction("fun foo() {}").bodyBlockExpression!!
    }

    fun createAnonymousInitializer(): KtAnonymousInitializer {
        return createClass("class A { init {} }").getAnonymousInitializers().first()
    }

    fun createEmptyClassBody(): KtClassBody {
        return createClass("class A(){}").getBody()!!
    }

    fun createParameter(@NonNls text: String): KtParameter {
        return createClass("class A($text)").primaryConstructorParameters.first()
    }

    fun createLoopParameter(@NonNls text: String): KtParameter {
        return (createExpression("for ($text in list) {}") as KtForExpression).loopParameter!!
    }

    fun createParameterList(@NonNls text: String): KtParameterList {
        return createFunction("fun foo$text{}").valueParameterList!!
    }

    fun createTypeParameterList(@NonNls text: String) = createClass("class Foo$text").typeParameterList!!

    fun createTypeParameter(@NonNls text: String) = createTypeParameterList("<$text>").parameters.first()!!

    fun createLambdaParameterListIfAny(@NonNls text: String) =
        createLambdaExpression(text, "0").functionLiteral.valueParameterList

    fun createLambdaParameterList(@NonNls text: String) = createLambdaParameterListIfAny(text)!!

    fun createLambdaExpression(@NonNls parameters: String, @NonNls body: String): KtLambdaExpression =
        (if (parameters.isNotEmpty()) createExpression("{ $parameters -> $body }")
        else createExpression("{ $body }")) as KtLambdaExpression


    fun createEnumEntry(@NonNls text: String): KtEnumEntry {
        return createDeclaration<KtClass>("enum class E {$text}").declarations[0] as KtEnumEntry
    }

    fun createEnumEntryInitializerList(): KtInitializerList {
        return createEnumEntry("Entry()").initializerList!!
    }

    fun createWhenEntry(@NonNls entryText: String): KtWhenEntry {
        val function = createFunction("fun foo() { when(12) { $entryText } }")
        val whenEntry = PsiTreeUtil.findChildOfType(function, KtWhenEntry::class.java)

        assert(whenEntry != null) { "Couldn't generate when entry" }
        assert(entryText == whenEntry!!.text) { "Generate when entry text differs from the given text" }

        return whenEntry
    }

    fun createWhenCondition(@NonNls conditionText: String): KtWhenCondition {
        val whenEntry = createWhenEntry("$conditionText -> {}")
        return whenEntry.conditions[0]
    }

    fun createBlockStringTemplateEntry(expression: KtExpression): KtStringTemplateEntryWithExpression {
        // We don't want reformatting here as it can potentially change something in raw strings
        val stringTemplateExpression = createExpressionByPattern("\"$\${$0}\"", expression, reformat = false) as KtStringTemplateExpression
        return stringTemplateExpression.entries[0] as KtStringTemplateEntryWithExpression
    }

    fun createSimpleNameStringTemplateEntry(@NonNls name: String): KtSimpleNameStringTemplateEntry {
        val stringTemplateExpression = createExpression("\"\$$name\"") as KtStringTemplateExpression
        return stringTemplateExpression.entries[0] as KtSimpleNameStringTemplateEntry
    }

    fun createLiteralStringTemplateEntry(@NonNls literal: String): KtLiteralStringTemplateEntry {
        val stringTemplateExpression = createExpression("\"$literal\"") as KtStringTemplateExpression
        return stringTemplateExpression.entries[0] as KtLiteralStringTemplateEntry
    }

    fun createStringTemplate(@NonNls content: String) = createExpression("\"$content\"") as KtStringTemplateExpression

    fun createPackageDirective(fqName: FqName): KtPackageDirective {
        return createFile("package ${fqName.asString()}").packageDirective!!
    }

    fun createPackageDirectiveIfNeeded(fqName: FqName): KtPackageDirective? {
        return if (fqName.isRoot) null else createPackageDirective(fqName)
    }

    fun createImportDirective(importPath: ImportPath): KtImportDirective {
        if (importPath.fqName.isRoot) {
            throw IllegalArgumentException("import path must not be empty")
        }

        val file = createFile(buildString { appendImport(importPath) })
        return file.importDirectives.first()
    }

    private fun StringBuilder.appendImport(importPath: ImportPath) {
        if (importPath.fqName.isRoot) {
            throw IllegalArgumentException("import path must not be empty")
        }

        append("import ")
        append(importPath.pathStr)

        val alias = importPath.alias
        if (alias != null) {
            append(" as ").append(alias.asString())
        }
    }

    @Deprecated("function is not used in the kotlin plugin/compiler and will be removed soon")
    fun createImportDirectives(paths: Collection<ImportPath>): List<KtImportDirective> {
        val fileContent = buildString {
            for (path in paths) {
                appendImport(path)
                append('\n')
            }
        }

        val file = createFile(fileContent)
        return file.importDirectives
    }

    fun createClassKeyword(): PsiElement = createClass("class A").getClassKeyword()!!

    fun createPrimaryConstructor(@NonNls text: String = ""): KtPrimaryConstructor {
        return createClass(if (text.isNotEmpty()) "class A $text" else "class A()").primaryConstructor!!
    }

    fun createPrimaryConstructorWithModifiers(@NonNls modifiers: String?): KtPrimaryConstructor {
        return modifiers?.let { createPrimaryConstructor("$it constructor()") } ?: createPrimaryConstructor()
    }

    fun createConstructorKeyword(): PsiElement =
        createClass("class A constructor()").primaryConstructor!!.getConstructorKeyword()!!

    fun createLabeledExpression(@NonNls labelName: String): KtLabeledExpression = createExpression("$labelName@ 1") as KtLabeledExpression

    fun createTypeCodeFragment(@NonNls text: String, context: PsiElement?): KtTypeCodeFragment {
        return KtTypeCodeFragment(project, "fragment.kt", text, context)
    }

    fun createExpressionCodeFragment(@NonNls text: String, context: PsiElement?): KtExpressionCodeFragment {
        return KtExpressionCodeFragment(project, "fragment.kt", text, null, context)
    }

    fun createBlockCodeFragment(@NonNls text: String, context: PsiElement?): KtBlockCodeFragment {
        return KtBlockCodeFragment(project, "fragment.kt", text, null, context)
    }

    fun createIf(condition: KtExpression, thenExpr: KtExpression, elseExpr: KtExpression? = null): KtIfExpression {
        return (if (elseExpr != null)
            createExpressionByPattern("if ($0) $1 else $2", condition, thenExpr, elseExpr) as KtIfExpression
        else
            createExpressionByPattern("if ($0) $1", condition, thenExpr)) as KtIfExpression
    }

    fun createArgument(
        expression: KtExpression?,
        name: Name? = null,
        isSpread: Boolean = false,
        reformat: Boolean = true
    ): KtValueArgument {
        val argumentList = buildByPattern(
            { pattern, args -> createByPattern(pattern, *args, reformat = reformat) { createCallArguments(it) } }) {
            appendFixedText("(")

            if (name != null) {
                val asString = name.asString()
                if (asString.isIdentifier()) {
                    appendName(name)
                } else {
                    appendFixedText("`$asString`")
                }
                appendFixedText(" = ")
            }

            if (isSpread) {
                appendFixedText("*")
            }

            appendExpression(expression)

            appendFixedText(")")
        }
        return argumentList.arguments.single()
    }

    fun createArgument(@NonNls text: String) = createCallArguments("($text)").arguments.first()!!

    fun createSuperTypeCallEntry(@NonNls text: String): KtSuperTypeCallEntry {
        return createClass("class A: $text").superTypeListEntries.first() as KtSuperTypeCallEntry
    }

    fun createSuperTypeEntry(@NonNls text: String): KtSuperTypeEntry {
        return createClass("class A: $text").superTypeListEntries.first() as KtSuperTypeEntry
    }

    fun creareDelegatedSuperTypeEntry(@NonNls text: String): KtConstructorDelegationCall {
        val colonOrEmpty = if (text.isEmpty()) "" else ": "
        return createClass("class A { constructor()$colonOrEmpty$text {}").secondaryConstructors.first().getDelegationCall()
    }

    class ClassHeaderBuilder {

        enum class State {
            MODIFIERS,
            NAME,
            TYPE_PARAMETERS,
            BASE_CLASS,
            TYPE_CONSTRAINTS,
            DONE
        }

        private val sb = StringBuilder()
        private var state = State.MODIFIERS

        fun modifier(@NonNls modifier: String): ClassHeaderBuilder {
            assert(state == State.MODIFIERS)

            sb.append(modifier)

            return this
        }

        private fun placeKeyword() {
            assert(state == State.MODIFIERS)

            if (sb.isNotEmpty()) {
                sb.append(" ")
            }
            sb.append("class ")

            state = State.NAME
        }


        fun name(@NonNls name: String): ClassHeaderBuilder {
            placeKeyword()

            sb.append(name)
            state = State.TYPE_PARAMETERS

            return this
        }

        private fun appendInAngleBrackets(values: Collection<String>) {
            if (values.isNotEmpty()) {
                sb.append(values.joinToString(", ", "<", ">"))
            }
        }

        fun typeParameters(values: Collection<String>): ClassHeaderBuilder {
            assert(state == State.TYPE_PARAMETERS)

            appendInAngleBrackets(values)
            state = State.BASE_CLASS

            return this
        }

        fun baseClass(@NonNls name: String, typeArguments: Collection<String>, isInterface: Boolean): ClassHeaderBuilder {
            assert(state == State.BASE_CLASS)

            sb.append(" : $name")
            appendInAngleBrackets(typeArguments)
            if (!isInterface) {
                sb.append("()")
            }

            state = State.TYPE_CONSTRAINTS

            return this
        }

        fun typeConstraints(values: Collection<String>): ClassHeaderBuilder {
            assert(state == State.TYPE_CONSTRAINTS)

            if (!values.isEmpty()) {
                sb.append(values.joinToString(", ", " where ", "", -1, ""))
            }
            state = State.DONE

            return this
        }

        fun transform(f: StringBuilder.() -> Unit) = sb.f()

        fun asString(): String {
            if (state != State.DONE) {
                state = State.DONE
            }

            return sb.toString()
        }
    }

    class CallableBuilder(private val target: Target) {

        companion object {
            val CONSTRUCTOR_NAME = KtTokens.CONSTRUCTOR_KEYWORD.value
        }

        enum class Target {
            FUNCTION,
            CONSTRUCTOR,
            READ_ONLY_PROPERTY
        }

        enum class State {
            MODIFIERS,
            NAME,
            RECEIVER,
            FIRST_PARAM,
            REST_PARAMS,
            TYPE_CONSTRAINTS,
            BODY,
            DONE
        }

        private val sb = StringBuilder()
        private var state = State.MODIFIERS

        private fun closeParams() {
            if (target == Target.FUNCTION || target == Target.CONSTRUCTOR) {
                assert(state == State.FIRST_PARAM || state == State.REST_PARAMS)
                sb.append(")")
            }

            state = State.TYPE_CONSTRAINTS
        }

        private fun placeKeyword() {
            assert(state == State.MODIFIERS)

            if (sb.isNotEmpty() && !sb.endsWith(" ")) {
                sb.append(" ")
            }
            val keyword = when (target) {
                Target.FUNCTION -> "fun"
                Target.CONSTRUCTOR -> ""
                Target.READ_ONLY_PROPERTY -> "val"
            }
            sb.append("$keyword ")

            state = State.RECEIVER
        }

        private fun bodyPrefix(breakLine: Boolean = true) = when (target) {
            Target.FUNCTION, Target.CONSTRUCTOR -> ""
            Target.READ_ONLY_PROPERTY -> (if (breakLine) "\n" else " ") + "get()"
        }

        fun modifier(modifier: String): CallableBuilder {
            assert(state == State.MODIFIERS)

            sb.append(modifier)

            return this
        }

        fun typeParams(values: Collection<String> = emptyList()): CallableBuilder {
            placeKeyword()
            if (!values.isEmpty()) {
                sb.append(values.joinToString(", ", "<", "> ", -1, ""))
            }

            return this
        }

        fun receiver(@NonNls receiverType: String): CallableBuilder {
            assert(state == State.RECEIVER)

            sb.append(receiverType).append(".")
            state = State.NAME

            return this
        }

        fun name(@NonNls name: String = CONSTRUCTOR_NAME): CallableBuilder {
            assert(state == State.NAME || state == State.RECEIVER)
            assert(name != CONSTRUCTOR_NAME || target == Target.CONSTRUCTOR)

            sb.append(name)
            state = when (target) {
                Target.FUNCTION, Target.CONSTRUCTOR -> {
                    sb.append("(")
                    State.FIRST_PARAM
                }
                else ->
                    State.TYPE_CONSTRAINTS
            }

            return this
        }

        fun param(@NonNls name: String, @NonNls type: String, @NonNls defaultValue: String? = null): CallableBuilder {
            assert(target == Target.FUNCTION || target == Target.CONSTRUCTOR)
            assert(state == State.FIRST_PARAM || state == State.REST_PARAMS)

            if (state == State.REST_PARAMS) {
                sb.append(", ")
            }
            sb.append(name).append(": ").append(type)
            if (defaultValue != null) {
                sb.append(" = ").append(defaultValue)
            }
            if (state == State.FIRST_PARAM) {
                state = State.REST_PARAMS
            }

            return this
        }

        fun returnType(@NonNls type: String): CallableBuilder {
            closeParams()
            sb.append(": ").append(type)

            return this
        }

        fun noReturnType(): CallableBuilder {
            closeParams()

            return this
        }

        fun typeConstraints(values: Collection<String>): CallableBuilder {
            assert(state == State.TYPE_CONSTRAINTS && target != Target.CONSTRUCTOR)

            if (!values.isEmpty()) {
                sb.append(values.joinToString(", ", " where ", "", -1, ""))
            }
            state = State.BODY

            return this
        }

        fun superDelegation(@NonNls argumentList: String): CallableBuilder {
            assert(state == State.TYPE_CONSTRAINTS && target == Target.CONSTRUCTOR)

            sb.append(": super").append(argumentList)
            state = State.BODY

            return this
        }

        fun blockBody(@NonNls body: String): CallableBuilder {
            assert(state == State.BODY || state == State.TYPE_CONSTRAINTS)

            sb.append(bodyPrefix()).append(" {\n").append(body).append("\n}")
            state = State.DONE

            return this
        }

        fun getterExpression(@NonNls expression: String, breakLine: Boolean = true): CallableBuilder {
            assert(target == Target.READ_ONLY_PROPERTY)
            assert(state == State.BODY || state == State.TYPE_CONSTRAINTS)

            sb.append(bodyPrefix(breakLine)).append(" = ").append(expression)
            state = State.DONE

            return this
        }

        fun initializer(@NonNls body: String): CallableBuilder {
            assert(target == Target.READ_ONLY_PROPERTY && (state == State.BODY || state == State.TYPE_CONSTRAINTS))

            sb.append(" = ").append(body)
            state = State.DONE

            return this
        }

        fun lazyBody(@NonNls body: String): CallableBuilder {
            assert(target == Target.READ_ONLY_PROPERTY && (state == State.BODY || state == State.TYPE_CONSTRAINTS))

            sb.append(" by kotlin.lazy {\n").append(body).append("\n}")
            state = State.DONE

            return this
        }

        fun transform(f: StringBuilder.() -> Unit) = sb.f()

        fun asString(): String {
            if (state != State.DONE) {
                state = State.DONE
            }

            return sb.toString()
        }
    }

    fun createBlock(@NonNls bodyText: String): KtBlockExpression {
        return createFunction("fun foo() {\n$bodyText\n}").bodyBlockExpression!!
    }

    fun createSingleStatementBlock(
        statement: KtExpression,
        @NonNls prevComment: String? = null,
        @NonNls nextComment: String? = null
    ): KtBlockExpression {
        val prev = if (prevComment == null) "" else " $prevComment "
        val next = if (nextComment == null) "" else " $nextComment "
        return createDeclarationByPattern<KtNamedFunction>("fun foo() {\n$prev$0$next\n}", statement).bodyBlockExpression!!
    }

    fun createComment(@NonNls text: String): PsiComment {
        val file = createFile(text)
        val comments = file.children.filterIsInstance<PsiComment>()
        val comment = comments.single()
        assert(comment.text == text)
        return comment
    }

    // special hack used in ControlStructureTypingVisitor
    // TODO: get rid of it
    fun wrapInABlockWrapper(expression: KtExpression): KtBlockExpression {
        if (expression is KtBlockExpression) {
            return expression
        }
        val function = createFunction("fun f() { ${expression.text} }")
        val block = function.bodyExpression as KtBlockExpression
        return BlockWrapper(block, expression)
    }

    private class BlockWrapper(fakeBlockExpression: KtBlockExpression, private val expression: KtExpression) :
        KtBlockExpression(fakeBlockExpression.text), KtPsiUtil.KtExpressionWrapper {

        override fun getStatements(): List<KtExpression> {
            return listOf(expression)
        }

        override fun getBaseExpression(): KtExpression {
            return expression
        }

        override fun getParent(): PsiElement = expression.parent

        override fun getPsiOrParent(): KtElement = expression.psiOrParent

        override fun getContainingKtFile() = expression.containingKtFile

        override fun getContainingFile(): PsiFile = expression.containingFile
    }
}
