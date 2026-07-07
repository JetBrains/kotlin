/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

/**
 * When non-`null`, marks this file as one that must not be analyzed; the string is a human-readable explanation. Set on
 * throwaway files created by [KtPsiFactory] without a context.
 */
var KtFile.doNotAnalyze: String? by UserDataProperty(Key.create("DO_NOT_ANALYZE"))

/**
 * The element whose context this file should be analyzed in, or `null` if the file has no analysis context. Set for
 * files created by a [contextual][KtPsiFactory.contextual] factory.
 */
var KtFile.analysisContext: PsiElement? by UserDataProperty(Key.create("ANALYSIS_CONTEXT"))

/**
 * A factory for creating Kotlin PSI elements programmatically, usually by parsing a snippet of source text.
 *
 * Most methods follow a `createX(text)` shape: they build a throwaway [KtFile] containing the given text and return the
 * requested element from it. Unless noted otherwise, the text must be a syntactically valid form of the requested
 * element, and the method throws if it cannot be produced; `...IfPossible` variants return `null` instead.
 *
 * The created elements are detached from any real file and are not meant to be analyzed, unless the factory is created
 * with a context via [contextual].
 *
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
        /**
         * Creates a factory bound to the given [context] element. Files created by such a factory carry [context] as
         * their analysis context, so the elements produced can be resolved as if they appeared at that location.
         */
        @JvmStatic
        @JvmOverloads
        fun contextual(context: PsiElement, markGenerated: Boolean = true, eventSystemEnabled: Boolean = false): KtPsiFactory {
            return KtPsiFactory(context.project, markGenerated, context, eventSystemEnabled)
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

    /** Creates a `val` keyword token. */
    fun createValKeyword(): PsiElement {
        val property = createProperty("val x = 1")
        return property.valOrVarKeyword
    }

    /** Creates a `var` keyword token. */
    fun createVarKeyword(): PsiElement {
        val property = createProperty("var x = 1")
        return property.valOrVarKeyword
    }

    private fun doCreateExpression(@NonNls text: String): KtExpression? {
        //NOTE: '\n' below is important - some strange code indenting problems appear without it
        return createProperty("val x =\n$text").initializer
    }

    /**
     * Creates an expression from the given [text]. Throws if the text cannot be parsed as a single expression whose
     * text matches [text] exactly; use [createExpressionIfPossible] to get `null` instead.
     */
    fun createExpression(@NonNls text: String): KtExpression {
        val expression = doCreateExpression(text) ?: error("Failed to create expression from text: '$text'")
        assert(expression.text == text) {
            "Failed to create expression from text: '$text', resulting expression's text was: '${expression.text}'"
        }
        return expression
    }

    /**
     * Creates an expression from the given [text], or returns `null` if the text cannot be parsed as a single
     * expression whose text matches [text] exactly.
     */
    fun createExpressionIfPossible(@NonNls text: String): KtExpression? {
        val expression = try {
            doCreateExpression(text) ?: return null
        } catch (ignored: Throwable) {
            return null
        }
        return if (expression.text == text) expression else null
    }

    /** Creates a `this` expression. */
    fun createThisExpression() =
        (createExpression("this.x") as KtQualifiedExpression).receiverExpression as KtThisExpression

    /** Creates a labeled `this` expression (`this@qualifier`). */
    fun createThisExpression(@NonNls qualifier: String) =
        (createExpression("this@$qualifier.x") as KtQualifiedExpression).receiverExpression as KtThisExpression

    /** Creates a value argument list from the given parenthesized [text] (for example, `"(1, x = 2)"`). */
    fun createCallArguments(@NonNls text: String): KtValueArgumentList {
        val property = createProperty("val x = foo $text")
        return (property.initializer as KtCallExpression).valueArgumentList!!
    }

    /** Creates a type argument list from the given angle-bracketed [text] (for example, `"<Int, String>"`). */
    fun createTypeArguments(@NonNls text: String): KtTypeArgumentList {
        val property = createProperty("val x = foo$text()")
        return (property.initializer as KtCallExpression).typeArgumentList!!
    }

    /** Creates a single type argument (projection) from the given [text]. */
    fun createTypeArgument(@NonNls text: String) = createTypeArguments("<$text>").arguments.first()

    /**
     * Creates a type reference from the given [type] text. Throws if the text cannot be parsed as a type whose text
     * matches [type] exactly; use [createTypeIfPossible] to get `null` instead.
     */
    fun createType(@NonNls type: String): KtTypeReference {
        val typeReference = createTypeIfPossible(type)
        if (typeReference == null || typeReference.text != type) {
            throw IllegalArgumentException("Incorrect type: $type")
        }
        return typeReference
    }

    /** Creates a type reference wrapping the given [typeElement]. */
    fun createType(typeElement: KtTypeElement) = createType("X").apply { this.typeElement!!.replace(typeElement) }

    /** Creates a type reference from the given [type] text, or returns `null` if it cannot be parsed as such. */
    fun createTypeIfPossible(@NonNls type: String): KtTypeReference? {
        val typeReference = createProperty("val x : $type").typeReference
        return if (typeReference?.text == type) typeReference else null
    }

    /** Creates a function-type receiver (the `T.` in `T.() -> R`) wrapping the given [typeReference]. */
    fun createFunctionTypeReceiver(typeReference: KtTypeReference): KtFunctionTypeReceiver {
        return (createType("A.() -> B").typeElement as KtFunctionType).receiver!!.apply { this.typeReference.replace(typeReference) }
    }

    /** Creates a function-type parameter whose type is the given [typeReference]. */
    fun createFunctionTypeParameter(typeReference: KtTypeReference): KtParameter {
        return (createType("(A) -> B").typeElement as KtFunctionType).parameters.first()
            .apply { this.typeReference!!.replace(typeReference) }
    }

    /** Creates a type alias with the given [name] and [typeParameters], aliasing the given [typeElement]. */
    fun createTypeAlias(@NonNls name: String, typeParameters: List<String>, typeElement: KtTypeElement): KtTypeAlias {
        return createTypeAlias(name, typeParameters, "X").apply { getTypeReference()!!.replace(createType(typeElement)) }
    }

    /** Creates a type alias with the given [name] and [typeParameters], aliasing the type in [body]. */
    fun createTypeAlias(@NonNls name: String, typeParameters: List<String>, @NonNls body: String): KtTypeAlias {
        val typeParametersText = if (typeParameters.isNotEmpty()) typeParameters.joinToString(prefix = "<", postfix = ">") else ""
        return createDeclaration("typealias $name$typeParametersText = $body")
    }

    /** Creates a `*` (star projection) token. */
    fun createStar(): PsiElement {
        return createType("List<*>").findElementAt(5)!!
    }

    /** Creates a comma token. */
    fun createComma(): PsiElement {
        return createType("T<X, Y>").findElementAt(3)!!
    }

    /** Creates a dot (`.`) token. */
    fun createDot(): PsiElement {
        return createType("T.(X)").findElementAt(1)!!
    }

    /** Creates a colon (`:`) token. */
    fun createColon(): PsiElement {
        return createProperty("val x: Int").findElementAt(5)!!
    }

    /** Creates an equals (`=`) token. */
    fun createEQ(): PsiElement {
        return createFunction("fun foo() = foo").equalsToken!!
    }

    /** Creates a semicolon token. */
    fun createSemicolon(): PsiElement {
        return createProperty("val x: Int;").findElementAt(10)!!
    }

    /** Creates a whitespace-then-arrow pair; the returned pair holds the first and last elements of that range. */
    fun createWhitespaceAndArrow(): Pair<PsiElement, PsiElement> {
        val functionType = createType("() -> Int").typeElement as KtFunctionType
        return Pair(functionType.findElementAt(2)!!, functionType.findElementAt(3)!!)
    }

    /** Creates a single-space whitespace element. */
    fun createWhiteSpace(): PsiElement {
        return createWhiteSpace(" ")
    }

    /** Creates a whitespace element with the exact given [text]. */
    fun createWhiteSpace(@NonNls text: String): PsiElement {
        return createProperty("val${text}x: Int").findElementAt(3)!!
    }

    /** Creates a whitespace element containing a single line break. */
    // Remove when all Java usages are rewritten to Kotlin
    fun createNewLine(): PsiElement {
        return createWhiteSpace("\n ")
    }

    /** Creates a whitespace element containing [lineBreaks] line breaks. */
    fun createNewLine(lineBreaks: Int): PsiElement {
        return createWhiteSpace("\n".repeat(lineBreaks))
    }

    /** Creates a class or interface declaration from the given [text]. */
    fun createClass(@NonNls text: String): KtClass {
        return createDeclaration(text)
    }

    /** Creates an object declaration from the given [text]. */
    fun createObject(@NonNls text: String): KtObjectDeclaration {
        return createDeclaration(text)
    }

    /** Creates an empty companion object. */
    fun createCompanionObject(): KtObjectDeclaration {
        return createCompanionObject("companion object {\n}")
    }

    /** Creates a companion object from the given [text] (for example, `"companion object { ... }"`). */
    fun createCompanionObject(@NonNls text: String): KtObjectDeclaration {
        return createClass("class A {\n $text\n}").companionObjects.first()
    }

    /** Creates a file-level annotation entry from the given [annotationText] (without the `@file:` prefix). */
    fun createFileAnnotation(@NonNls annotationText: String): KtAnnotationEntry {
        return createFileAnnotationListWithAnnotation(annotationText).annotationEntries.first()
    }

    /** Creates a file annotation list containing a single `@file:` annotation from the given [annotationText]. */
    fun createFileAnnotationListWithAnnotation(@NonNls annotationText: String): KtFileAnnotationList {
        return createFile("@file:$annotationText").fileAnnotationList!!
    }

    /** Creates a [KtFile] with the given [text] and a dummy file name. */
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

    /**
     * Creates a [KtFile] with the given [fileName] and [text]. If this factory has a context, the file is marked
     * analyzable in that context; otherwise it is marked as [do-not-analyze][doNotAnalyze].
     */
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

    /**
     * Creates a physical (event-system-enabled) [KtFile] with the given [fileName] and [text]. Unlike [createFile],
     * the resulting file participates in the PSI event system and supports light classes.
     */
    fun createPhysicalFile(@NonNls fileName: String, @NonNls text: String): KtFile {
        val time = LocalTimeCounter.currentTime()
        val file = PsiFileFactory.getInstance(project).createFileFromText(fileName, KotlinFileType.INSTANCE, text, time, true) as KtFile
        file.analysisContext = this@KtPsiFactory.context
        return file
    }

    /**
     * Creates a REPL snippet [KtScript] from the specified text content.
     */
    @KtExperimentalApi
    @OptIn(KtNonPublicApi::class)
    fun createReplSnippet(@NonNls text: String): KtScript {
        val file = doCreateFile("snippet.repl.kts", text)
        val script = file.script!!
        script.markAsReplSnippet()
        return script
    }

    /**
     * Creates a property from its parts: optional [modifiers], [name], optional [type], mutability ([isVar]), and
     * optional [initializer].
     */
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

    /** Creates a property from its parts, without modifiers. */
    fun createProperty(@NonNls name: String, @NonNls type: String?, isVar: Boolean, @NonNls initializer: String?): KtProperty {
        return createProperty(null, name, type, isVar, initializer)
    }

    /** Creates a property with the given [name] and optional [type] and no initializer. */
    fun createProperty(@NonNls name: String, @NonNls type: String?, isVar: Boolean): KtProperty {
        return createProperty(name, type, isVar, null)
    }

    /** Creates a property from the given full declaration [text] (for example, `"val x: Int = 1"`). */
    fun createProperty(@NonNls text: String): KtProperty {
        return createDeclaration(text)
    }

    /** Creates a property getter whose body is the given [expression] (a block or a single expression). */
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

    /** Creates a property setter whose body is the given [expression] (a block or a single expression). */
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

    /** Creates a property delegate (`by ...`) whose delegate expression is the given [expression]. */
    fun createPropertyDelegate(expression: KtExpression): KtPropertyDelegate {
        val property = createProperty("val x by lazy { 1 }")
        val delegate = property.delegate!!
        val delegateExpression = delegate.expression!!
        delegateExpression.replace(expression)
        return delegate
    }

    /** Creates a destructuring declaration from the given [text] (for example, `"val (x, y) = pair"`). */
    fun createDestructuringDeclaration(@NonNls text: String): KtDestructuringDeclaration {
        return createFunction("fun foo() {$text}").bodyBlockExpression!!.statements.first() as KtDestructuringDeclaration
    }

    /** Creates a destructuring lambda parameter from the given [text] (for example, `"(x, y)"`). */
    fun createDestructuringParameter(@NonNls text: String): KtParameter {
        val dummyFun = createFunction("fun foo() = { $text -> }")
        return (dummyFun.bodyExpression as KtLambdaExpression).functionLiteral.valueParameters.first()
    }

    /**
     * Creates a top-level declaration from the given [text], cast to the expected type [TDeclaration]. The text must
     * contain exactly one declaration.
     */
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

    /** Creates a name identifier token for the given [name]. Throws if [name] is not a valid identifier. */
    fun createNameIdentifier(@NonNls name: String) = createNameIdentifierIfPossible(name)!!

    /** Creates a name identifier token for the given [name], or returns `null` if [name] is not a valid identifier. */
    fun createNameIdentifierIfPossible(@NonNls name: String) = createProperty(name, null, false).nameIdentifier

    /** Creates a simple name reference expression for the given [name]. */
    fun createSimpleName(@NonNls name: String): KtSimpleNameExpression {
        return createProperty(name, null, false, name).initializer as KtSimpleNameExpression
    }

    /** Creates an operation-reference expression for the given operator [name] (for example, `"+"`). */
    fun createOperationName(@NonNls name: String): KtSimpleNameExpression {
        return (createExpression("0 $name 0") as KtBinaryExpression).operationReference
    }

    /** Creates a bare identifier token for the given [name]. */
    fun createIdentifier(@NonNls name: String): PsiElement {
        return createSimpleName(name).getIdentifier()!!
    }

    /** Creates a named function from the given declaration text [funDecl]. */
    fun createFunction(@NonNls funDecl: String): KtNamedFunction {
        return createDeclaration(funDecl)
    }

    /** Creates a callable reference expression from the given [text], or returns `null` if the text is not one. */
    fun createCallableReferenceExpression(@NonNls text: String) = createExpression(text) as? KtCallableReferenceExpression

    /** Creates a secondary constructor from the given declaration text [decl]. */
    fun createSecondaryConstructor(@NonNls decl: String): KtSecondaryConstructor {
        return createClass("class Foo {\n $decl \n}").secondaryConstructors.first()
    }

    /** Creates a modifier list containing the single given [modifier] keyword. */
    fun createModifierList(modifier: KtModifierKeywordToken): KtModifierList {
        return createModifierList(modifier.value)
    }

    /** Creates a modifier list from the given [text] (a space-separated sequence of modifiers). */
    fun createModifierList(@NonNls text: String): KtModifierList {
        return createClass("$text interface x").modifierList!!
    }

    /** Creates an empty modifier list. */
    fun createEmptyModifierList() = createModifierList(KtTokens.PRIVATE_KEYWORD).apply { firstChild.delete() }

    /** Creates a single modifier keyword token. */
    fun createModifier(modifier: KtModifierKeywordToken): PsiElement {
        return createModifierList(modifier.value).getModifier(modifier)!!
    }

    /** Creates an annotation entry from the given [text] (for example, `"@Suppress(\"x\")"`). */
    fun createAnnotationEntry(@NonNls text: String): KtAnnotationEntry {
        val modifierList = createProperty(text + " val x").modifierList
        return modifierList!!.annotationEntries.first()
    }

    /** Creates an empty function body block (`{}`). */
    fun createEmptyBody(): KtBlockExpression {
        return createFunction("fun foo() {}").bodyBlockExpression!!
    }

    /** Creates an empty `init` block. */
    fun createAnonymousInitializer(): KtAnonymousInitializer {
        return createClass("class A { init {} }").getAnonymousInitializers().first()
    }

    /** Creates an empty class body (`{}`). */
    fun createEmptyClassBody(): KtClassBody {
        return createClass("class A(){}").getBody()!!
    }

    /** Creates a value parameter from the given [text] (for example, `"x: Int = 0"`). */
    fun createParameter(@NonNls text: String): KtParameter {
        return createClass("class A($text)").primaryConstructorParameters.first()
    }

    /** Creates a `for`-loop parameter from the given [text]. */
    fun createLoopParameter(@NonNls text: String): KtParameter {
        return (createExpression("for ($text in list) {}") as KtForExpression).loopParameter!!
    }

    /** Creates a parameter list from the given parenthesized [text] (for example, `"(x: Int, y: Int)"`). */
    fun createParameterList(@NonNls text: String): KtParameterList {
        return createFunction("fun foo$text{}").valueParameterList!!
    }

    /** Creates a type parameter list from the given angle-bracketed [text] (for example, `"<T, R>"`). */
    fun createTypeParameterList(@NonNls text: String) = createClass("class Foo$text").typeParameterList!!

    /** Creates a single type parameter from the given [text] (for example, `"T : Comparable<T>"`). */
    fun createTypeParameter(@NonNls text: String) = createTypeParameterList("<$text>").parameters.first()!!

    /** Creates the parameter list of a lambda with the given parameter [text], or `null` if the lambda declares none. */
    fun createLambdaParameterListIfAny(@NonNls text: String) =
        createLambdaExpression(text, "0").functionLiteral.valueParameterList

    /** Creates the parameter list of a lambda with the given parameter [text]. */
    fun createLambdaParameterList(@NonNls text: String) = createLambdaParameterListIfAny(text)!!

    /** Creates a lambda expression with the given [parameters] and [body] text. */
    fun createLambdaExpression(@NonNls parameters: String, @NonNls body: String): KtLambdaExpression =
        (if (parameters.isNotEmpty()) createExpression("{ $parameters -> $body }")
        else createExpression("{ $body }")) as KtLambdaExpression


    /** Creates an enum entry from the given [text] (for example, `"RED"` or `"RED(0xFF0000)"`). */
    fun createEnumEntry(@NonNls text: String): KtEnumEntry {
        return createDeclaration<KtClass>("enum class E {$text}").declarations[0] as KtEnumEntry
    }

    /** Creates an enum entry initializer list holding a single set of constructor arguments. */
    fun createEnumEntryInitializerList(): KtInitializerList {
        return createEnumEntry("Entry()").initializerList!!
    }

    /**
     * Creates a `when` entry from the given [entryText] (for example, `"1 -> \"one\""`). Throws if the produced entry's
     * text does not match [entryText].
     */
    fun createWhenEntry(@NonNls entryText: String): KtWhenEntry {
        val function = createFunction("fun foo() { when(12) { $entryText } }")
        val whenEntry = PsiTreeUtil.findChildOfType(function, KtWhenEntry::class.java)

        assert(whenEntry != null) { "Couldn't generate when entry" }
        assert(entryText == whenEntry!!.text) { "Generate when entry text differs from the given text" }

        return whenEntry
    }

    /** Creates a `when` condition from the given [conditionText] (for example, `"in 1..10"` or `"is String"`). */
    fun createWhenCondition(@NonNls conditionText: String): KtWhenCondition {
        val whenEntry = createWhenEntry("$conditionText -> {}")
        return whenEntry.conditions[0]
    }

    /** Creates a block string-template entry (`${...}`) wrapping the given [expression]. */
    fun createBlockStringTemplateEntry(expression: KtExpression): KtStringTemplateEntryWithExpression {
        // We don't want reformatting here as it can potentially change something in raw strings
        val stringTemplateExpression = createExpressionByPattern("\"$\${$0}\"", expression, reformat = false) as KtStringTemplateExpression
        return stringTemplateExpression.entries[0] as KtStringTemplateEntryWithExpression
    }

    /**
     * Creates a block string-template entry (`${...}`) wrapping the given [expression], for a string template whose
     * interpolation prefix is [prefixLength] dollar signs (for example, `$${...}` when [prefixLength] is `2`).
     */
    fun createMultiDollarBlockStringTemplateEntry(expression: KtExpression, prefixLength: Int): KtStringTemplateEntryWithExpression {
        checkInterpolationPrefixLength(prefixLength)
        // '$' is a special character, the second '$' is necessary for escaping it in the pattern. See createExpressionByPattern
        val prefix = "$$".repeat(prefixLength)
        val stringTemplateExpression =
            createExpressionByPattern("$prefix\"$prefix{$0}\"", expression, reformat = false) as KtStringTemplateExpression
        return stringTemplateExpression.entries[0] as KtStringTemplateEntryWithExpression
    }

    /** Creates a simple-name string-template entry (`$name`) for the given [name]. */
    fun createSimpleNameStringTemplateEntry(@NonNls name: String): KtSimpleNameStringTemplateEntry {
        val stringTemplateExpression = createExpression("\"\$$name\"") as KtStringTemplateExpression
        return stringTemplateExpression.entries[0] as KtSimpleNameStringTemplateEntry
    }

    /**
     * Creates a simple-name string-template entry (`$name`) for the given [name], for a string template whose
     * interpolation prefix is [prefixLength] dollar signs (for example, `$$name` when [prefixLength] is `2`).
     */
    fun createMultiDollarSimpleNameStringTemplateEntry(@NonNls name: String, prefixLength: Int): KtSimpleNameStringTemplateEntry {
        checkInterpolationPrefixLength(prefixLength)
        val prefix = "$".repeat(prefixLength)
        return createMultiDollarStringTemplate("$prefix$name", prefixLength).entries[0] as KtSimpleNameStringTemplateEntry
    }

    /** Creates a literal (plain text) string-template entry for the given [literal] text. */
    fun createLiteralStringTemplateEntry(@NonNls literal: String): KtLiteralStringTemplateEntry {
        val stringTemplateExpression = createExpression("\"$literal\"") as KtStringTemplateExpression
        return stringTemplateExpression.entries[0] as KtLiteralStringTemplateEntry
    }

    /** Creates a regular (double-quoted) string template with the given [content]. */
    fun createStringTemplate(@NonNls content: String) = createExpression("\"$content\"") as KtStringTemplateExpression

    /** Creates a raw (triple-quoted) string template with the given [content]. */
    fun createRawStringTemplate(@NonNls content: String): KtStringTemplateExpression {
        val quote = "\"\"\""
        return createExpression("$quote$content$quote") as KtStringTemplateExpression
    }

    /**
     * Creates a string template with the given [content] whose interpolation prefix is [prefixLength] dollar signs. A
     * raw (triple-quoted) template is used when [content] spans multiple lines or [forceMultiQuoted] is `true`, and a
     * regular (double-quoted) one otherwise.
     */
    fun createMultiDollarStringTemplate(
        @NonNls content: String,
        prefixLength: Int,
        forceMultiQuoted: Boolean = false,
    ): KtStringTemplateExpression {
        checkInterpolationPrefixLength(prefixLength)
        val quote = if (content.lines().size > 1 || forceMultiQuoted) "\"\"\"" else "\""
        val prefix = "$".repeat(prefixLength)
        return createExpression("$prefix$quote$content$quote") as KtStringTemplateExpression
    }

    private fun checkInterpolationPrefixLength(prefixLength: Int) {
        check(prefixLength > 0) { "Interpolation prefix length should be more than 0, got $prefixLength" }
    }

    /** Creates a `package` directive for the given [fqName]. */
    fun createPackageDirective(fqName: FqName): KtPackageDirective {
        return createFile("package ${fqName.asString()}").packageDirective!!
    }

    /** Creates a `package` directive for the given [fqName], or returns `null` for the root package. */
    fun createPackageDirectiveIfNeeded(fqName: FqName): KtPackageDirective? {
        return if (fqName.isRoot) null else createPackageDirective(fqName)
    }

    /**
     * Creates an `import` directive for the given [importPath] (including its all-under flag and alias).
     *
     * @throws IllegalArgumentException if the import path is empty (the root package)
     */
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

    /** Creates a `class` keyword token. */
    fun createClassKeyword(): PsiElement = createClass("class A").getClassKeyword()!!

    /** Creates a primary constructor from the given [text] (for example, `"constructor(x: Int)"`), defaulting to an empty one. */
    fun createPrimaryConstructor(@NonNls text: String = ""): KtPrimaryConstructor {
        return createClass(if (text.isNotEmpty()) "class A $text" else "class A()").primaryConstructor!!
    }

    /** Creates an empty primary constructor carrying the given [modifiers]. */
    fun createPrimaryConstructorWithModifiers(@NonNls modifiers: String?): KtPrimaryConstructor {
        return modifiers?.let { createPrimaryConstructor("$it constructor()") } ?: createPrimaryConstructor()
    }

    /** Creates a `constructor` keyword token. */
    fun createConstructorKeyword(): PsiElement =
        createClass("class A constructor()").primaryConstructor!!.getConstructorKeyword()!!

    /** Creates a labeled expression with the given [labelName] (for example, `"loop@ 1"`). */
    fun createLabeledExpression(@NonNls labelName: String): KtLabeledExpression = createExpression("$labelName@ 1") as KtLabeledExpression

    /** Creates a [KtTypeCodeFragment] for the given [text], resolved in the given [context]. */
    fun createTypeCodeFragment(@NonNls text: String, context: PsiElement?): KtTypeCodeFragment {
        return KtTypeCodeFragment(project, "fragment.kt", text, context)
    }

    /** Creates a [KtExpressionCodeFragment] for the given [text], resolved in the given [context]. */
    fun createExpressionCodeFragment(@NonNls text: String, context: PsiElement?): KtExpressionCodeFragment {
        return KtExpressionCodeFragment(project, "fragment.kt", text, null, context)
    }

    /** Creates a [KtBlockCodeFragment] for the given [text], resolved in the given [context]. */
    fun createBlockCodeFragment(@NonNls text: String, context: PsiElement?): KtBlockCodeFragment {
        return KtBlockCodeFragment(project, "fragment.kt", text, null, context)
    }

    /** Creates an `if` expression with the given [condition], [thenExpr], and optional [elseExpr]. */
    fun createIf(condition: KtExpression, thenExpr: KtExpression, elseExpr: KtExpression? = null): KtIfExpression {
        return (if (elseExpr != null)
            createExpressionByPattern("if ($0) $1 else $2", condition, thenExpr, elseExpr) as KtIfExpression
        else
            createExpressionByPattern("if ($0) $1", condition, thenExpr)) as KtIfExpression
    }

    /**
     * Creates a value argument wrapping the given [expression], optionally named ([name]) and/or spread ([isSpread]).
     *
     * @param reformat whether to reformat the created argument
     */
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

    /** Creates a value argument from the given [text] (for example, `"x = 1"` or `"*array"`). */
    fun createArgument(@NonNls text: String) = createCallArguments("($text)").arguments.first()!!

    /** Creates a superclass constructor call entry from the given [text] (for example, `"Base(1)"`). */
    fun createSuperTypeCallEntry(@NonNls text: String): KtSuperTypeCallEntry {
        return createClass("class A: $text").superTypeListEntries.first() as KtSuperTypeCallEntry
    }

    /** Creates a plain supertype entry from the given [text] (for example, an interface name `"Runnable"`). */
    fun createSuperTypeEntry(@NonNls text: String): KtSuperTypeEntry {
        return createClass("class A: $text").superTypeListEntries.first() as KtSuperTypeEntry
    }

    /** Creates a constructor delegation call from the given [text] (for example, `"super(1)"` or `"this()"`). */
    fun creareDelegatedSuperTypeEntry(@NonNls text: String): KtConstructorDelegationCall {
        val colonOrEmpty = if (text.isEmpty()) "" else ": "
        return createClass("class A { constructor()$colonOrEmpty$text {}").secondaryConstructors.first().getDelegationCall()
    }

    /**
     * A fluent builder for a class header string, assembled in a fixed order: modifiers, name, type parameters, base
     * class, and type constraints. Call [asString] to obtain the resulting header text, then pass it to
     * [createClass] to build the PSI.
     */
    class ClassHeaderBuilder {

        /** The stages of building a class header, enforced in order by the builder. */
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

        /** Appends a modifier. Must be called before [name]. */
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


        /** Appends the `class` keyword and the class [name]. Ends the modifiers stage. */
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

        /** Appends the type parameters, if any. */
        fun typeParameters(values: Collection<String>): ClassHeaderBuilder {
            assert(state == State.TYPE_PARAMETERS)

            appendInAngleBrackets(values)
            state = State.BASE_CLASS

            return this
        }

        /**
         * Appends the base class or interface: its [name] and [typeArguments]. When [isInterface] is `false`, an empty
         * constructor call `()` is appended.
         */
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

        /** Appends the `where` clause type constraints, if any. */
        fun typeConstraints(values: Collection<String>): ClassHeaderBuilder {
            assert(state == State.TYPE_CONSTRAINTS)

            if (!values.isEmpty()) {
                sb.append(values.joinToString(", ", " where ", "", -1, ""))
            }
            state = State.DONE

            return this
        }

        /** Applies an arbitrary transformation to the underlying text buffer. */
        fun transform(f: StringBuilder.() -> Unit) = sb.f()

        /** Returns the assembled class header text. */
        fun asString(): String {
            if (state != State.DONE) {
                state = State.DONE
            }

            return sb.toString()
        }
    }

    /**
     * A fluent builder for a function, constructor, or read-only property declaration string. The available steps and
     * their order depend on the [Target]; call [asString] to obtain the resulting text, then pass it to the matching
     * `create*` method.
     */
    class CallableBuilder(private val target: Target) {

        companion object {
            /** The pseudo-name used for constructors (the `constructor` keyword). */
            val CONSTRUCTOR_NAME = KtTokens.CONSTRUCTOR_KEYWORD.value
        }

        /** The kind of callable being built. */
        enum class Target {
            FUNCTION,
            CONSTRUCTOR,
            READ_ONLY_PROPERTY
        }

        /** The stages of building a callable, enforced in order by the builder. */
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

        /** Appends a modifier. Must be called before [typeParams]. */
        fun modifier(modifier: String): CallableBuilder {
            assert(state == State.MODIFIERS)

            sb.append(modifier)

            return this
        }

        /** Appends the declaration keyword (`fun`/`val`) and the type parameters, if any. */
        fun typeParams(values: Collection<String> = emptyList()): CallableBuilder {
            placeKeyword()
            if (!values.isEmpty()) {
                sb.append(values.joinToString(", ", "<", "> ", -1, ""))
            }

            return this
        }

        /** Appends the extension receiver type. */
        fun receiver(@NonNls receiverType: String): CallableBuilder {
            assert(state == State.RECEIVER)

            sb.append(receiverType).append(".")
            state = State.NAME

            return this
        }

        /** Appends the callable name (defaulting to the constructor pseudo-name), opening the parameter list for functions and constructors. */
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

        /** Appends a value parameter with the given [name], [type], and optional [defaultValue]. Functions and constructors only. */
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

        /** Closes the parameter list (if any) and appends the return [type]. */
        fun returnType(@NonNls type: String): CallableBuilder {
            closeParams()
            sb.append(": ").append(type)

            return this
        }

        /** Closes the parameter list (if any) without appending a return type. */
        fun noReturnType(): CallableBuilder {
            closeParams()

            return this
        }

        /** Appends the `where` clause type constraints, if any. Not allowed for constructors. */
        fun typeConstraints(values: Collection<String>): CallableBuilder {
            assert(state == State.TYPE_CONSTRAINTS && target != Target.CONSTRUCTOR)

            if (!values.isEmpty()) {
                sb.append(values.joinToString(", ", " where ", "", -1, ""))
            }
            state = State.BODY

            return this
        }

        /** Appends a `: super(...)` delegation call with the given [argumentList]. Constructors only. */
        fun superDelegation(@NonNls argumentList: String): CallableBuilder {
            assert(state == State.TYPE_CONSTRAINTS && target == Target.CONSTRUCTOR)

            sb.append(": super").append(argumentList)
            state = State.BODY

            return this
        }

        /** Appends a block body containing the given [body] text (a getter block for a property). */
        fun blockBody(@NonNls body: String): CallableBuilder {
            assert(state == State.BODY || state == State.TYPE_CONSTRAINTS)

            sb.append(bodyPrefix()).append(" {\n").append(body).append("\n}")
            state = State.DONE

            return this
        }

        /** Appends an expression-body getter (`get() = expression`). Read-only properties only. */
        fun getterExpression(@NonNls expression: String, breakLine: Boolean = true): CallableBuilder {
            assert(target == Target.READ_ONLY_PROPERTY)
            assert(state == State.BODY || state == State.TYPE_CONSTRAINTS)

            sb.append(bodyPrefix(breakLine)).append(" = ").append(expression)
            state = State.DONE

            return this
        }

        /** Appends an initializer (`= body`). Read-only properties only. */
        fun initializer(@NonNls body: String): CallableBuilder {
            assert(target == Target.READ_ONLY_PROPERTY && (state == State.BODY || state == State.TYPE_CONSTRAINTS))

            sb.append(" = ").append(body)
            state = State.DONE

            return this
        }

        /** Appends a lazy delegate (`by kotlin.lazy { body }`). Read-only properties only. */
        fun lazyBody(@NonNls body: String): CallableBuilder {
            assert(target == Target.READ_ONLY_PROPERTY && (state == State.BODY || state == State.TYPE_CONSTRAINTS))

            sb.append(" by kotlin.lazy {\n").append(body).append("\n}")
            state = State.DONE

            return this
        }

        /** Applies an arbitrary transformation to the underlying text buffer. */
        fun transform(f: StringBuilder.() -> Unit) = sb.f()

        /** Returns the assembled callable declaration text. */
        fun asString(): String {
            if (state != State.DONE) {
                state = State.DONE
            }

            return sb.toString()
        }
    }

    /** Creates a function body block wrapping the given [bodyText]. */
    fun createBlock(@NonNls bodyText: String): KtBlockExpression {
        return createFunction("fun foo() {\n$bodyText\n}").bodyBlockExpression!!
    }

    /**
     * Creates a block containing the single given [statement], optionally preceded by [prevComment] and followed by
     * [nextComment].
     */
    fun createSingleStatementBlock(
        statement: KtExpression,
        @NonNls prevComment: String? = null,
        @NonNls nextComment: String? = null
    ): KtBlockExpression {
        val prev = if (prevComment == null) "" else " $prevComment "
        val next = if (nextComment == null) "" else " $nextComment "
        return createDeclarationByPattern<KtNamedFunction>("fun foo() {\n$prev$0$next\n}", statement).bodyBlockExpression!!
    }

    /** Creates a comment (line or block) from the given [text]; the text must be a single comment. */
    fun createComment(@NonNls text: String): PsiComment {
        val file = createFile(text)
        val comments = file.children.filterIsInstance<PsiComment>()
        val comment = comments.single()
        assert(comment.text == text)
        return comment
    }

    /**
     * Wraps the given [expression] in a synthetic single-statement block. If [expression] is already a block, it is
     * returned unchanged. Intended for internal compiler use.
     */
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
