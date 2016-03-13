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

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.LocalTimeCounter
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.ImportPath

fun KtPsiFactory(project: Project?): KtPsiFactory = KtPsiFactory(project!!)
fun KtPsiFactory(elementForProject: PsiElement): KtPsiFactory = KtPsiFactory(elementForProject.project)

private val DO_NOT_ANALYZE_NOTIFICATION = "This file was created by KtPsiFactory and should not be analyzed\n" +
                                          "Use createAnalyzableFile to create file that can be analyzed\n"

var KtFile.doNotAnalyze: String? by UserDataProperty(Key.create("DO_NOT_ANALYZE"))
var KtFile.analysisContext: PsiElement? by UserDataProperty(Key.create("ANALYSIS_CONTEXT"))
var PsiFile.moduleInfo: ModuleInfo? by UserDataProperty(Key.create("MODULE_INFO"))

class KtPsiFactory(private val project: Project) {

    fun createValKeyword(): PsiElement {
        val property = createProperty("val x = 1")
        return property.getValOrVarKeyword()
    }

    fun createVarKeyword(): PsiElement {
        val property = createProperty("var x = 1")
        return property.getValOrVarKeyword()
    }

    fun createSafeCallNode(): ASTNode {
        return (createExpression("a?.b") as KtSafeQualifiedExpression).operationTokenNode
    }

    private fun doCreateExpression(text: String): KtExpression {
        //TODO: '\n' below if important - some strange code indenting problems appear without it
        val expression = createProperty("val x =\n$text").getInitializer() ?: error("Failed to create expression from text: '$text'")
        return expression
    }

    fun createExpression(text: String): KtExpression {
        val expression = doCreateExpression(text)
        assert(expression.text == text) {
            "Failed to create expression from text: '$text', resulting expression's text was: '${expression.text}'"
        }
        return expression
    }

    fun createExpressionIfPossible(text: String): KtExpression? {
        val expression = doCreateExpression(text)
        return if (expression.text == text) expression else null
    }

    fun createClassLiteral(className: String): KtClassLiteralExpression =
            createExpression("$className::class") as KtClassLiteralExpression

    fun createCallArguments(text: String): KtValueArgumentList {
        val property = createProperty("val x = foo $text")
        return (property.getInitializer() as KtCallExpression).valueArgumentList!!
    }

    fun createTypeArguments(text: String): KtTypeArgumentList {
        val property = createProperty("val x = foo$text()")
        return (property.getInitializer() as KtCallExpression).typeArgumentList!!
    }

    fun createType(type: String): KtTypeReference {
        val typeReference = createTypeIfPossible(type)
        if (typeReference == null || typeReference.text != type) {
            throw IllegalArgumentException("Incorrect type: $type")
        }
        return typeReference
    }

    fun createTypeIfPossible(type: String): KtTypeReference? {
        val typeReference = createProperty("val x : $type").typeReference
        return if (typeReference?.text == type) typeReference else null
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
        return createFunction("fun foo() = foo").getEqualsToken()!!
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

    fun createWhiteSpace(text: String): PsiElement {
        return createProperty("val${text}x").findElementAt(3)!!
    }

    // Remove when all Java usages are rewritten to Kotlin
    fun createNewLine(): PsiElement {
        return createWhiteSpace("\n")
    }

    fun createNewLine(lineBreaks: Int): PsiElement {
        return createWhiteSpace("\n".repeat(lineBreaks))
    }

    fun createClass(text: String): KtClass {
        return createDeclaration(text)
    }

    fun createCompanionObject(): KtObjectDeclaration {
        return createClass("class A {\n companion object{\n}\n}").getCompanionObjects().first()
    }

    fun createFileAnnotation(annotationText: String): KtAnnotationEntry {
        return createFileAnnotationListWithAnnotation(annotationText).annotationEntries.first()
    }

    fun createFileAnnotationListWithAnnotation(annotationText: String) : KtFileAnnotationList {
        return createFile("@file:${annotationText}").fileAnnotationList!!
    }

    fun createFile(text: String): KtFile {
        return createFile("dummy.kt", text)
    }

    private fun doCreateFile(fileName: String, text: String): KtFile {
        return PsiFileFactory.getInstance(project).createFileFromText(fileName, KotlinFileType.INSTANCE, text, LocalTimeCounter.currentTime(), false) as KtFile
    }

    fun createFile(fileName: String, text: String): KtFile {
        val file = doCreateFile(fileName, text)

        file.doNotAnalyze = DO_NOT_ANALYZE_NOTIFICATION

        return file
    }

    fun createAnalyzableFile(fileName: String, text: String, contextToAnalyzeIn: PsiElement): KtFile {
        val file = doCreateFile(fileName, text)
        file.analysisContext = contextToAnalyzeIn
        return file
    }

    fun createFileWithLightClassSupport(fileName: String, text: String, contextToAnalyzeIn: PsiElement): KtFile {
        val file = createPhysicalFile(fileName, text)
        file.analysisContext = contextToAnalyzeIn
        return file
    }

    fun createPhysicalFile(fileName: String, text: String): KtFile {
        return PsiFileFactory.getInstance(project).createFileFromText(fileName, KotlinFileType.INSTANCE, text, LocalTimeCounter.currentTime(), true) as KtFile
    }

    fun createProperty(name: String, type: String?, isVar: Boolean, initializer: String?): KtProperty {
        val text = (if (isVar) "var " else "val ") + name + (if (type != null) ":" + type else "") + (if (initializer == null) "" else " = " + initializer)
        return createProperty(text)
    }

    fun createProperty(name: String, type: String?, isVar: Boolean): KtProperty {
        return createProperty(name, type, isVar, null)
    }

    fun createProperty(text: String): KtProperty {
        return createDeclaration(text)
    }

    fun createPropertyGetter(expression: KtExpression): KtPropertyAccessor {
        val property = createProperty("val x get() = 1")
        val getter = property.getter!!
        val bodyExpression = getter.bodyExpression!!

        bodyExpression.replace(expression)
        return getter
    }

    fun createDestructuringDeclaration(text: String): KtDestructuringDeclaration {
        return (createFunction("fun foo() {$text}").bodyExpression as KtBlockExpression).statements.first() as KtDestructuringDeclaration
    }

    fun createDestructuringDeclarationInFor(text: String): KtDestructuringDeclaration {
        return ((createFunction("fun foo() {for ($text in foo) {} }").bodyExpression as KtBlockExpression).statements.first() as KtForExpression).destructuringParameter!!
    }

    fun createDestructuringParameter(text: String): KtDestructuringDeclaration {
        val dummyFun = createFunction("fun foo() { for ($text in foo) {} }")
        return ((dummyFun.bodyExpression as KtBlockExpression).statements.first() as KtForExpression).destructuringParameter!!
    }

    fun <TDeclaration : KtDeclaration> createDeclaration(text: String): TDeclaration {
        val file = createFile(text)
        val declarations = file.declarations
        assert(declarations.size == 1) { "${declarations.size} declarations in $text" }
        @Suppress("UNCHECKED_CAST")
        val result = declarations.first() as TDeclaration
        return result
    }

    fun createNameIdentifier(name: String): PsiElement {
        return createProperty(name, null, false).getNameIdentifier()!!
    }

    fun createSimpleName(name: String): KtSimpleNameExpression {
        return createProperty(name, null, false, name).getInitializer() as KtSimpleNameExpression
    }

    fun createOperationName(name: String): KtSimpleNameExpression {
        return (createExpression("0 $name 0") as KtBinaryExpression).operationReference
    }

    fun createIdentifier(name: String): PsiElement {
        return createSimpleName(name).getIdentifier()!!
    }

    fun createFunction(funDecl: String): KtNamedFunction {
        return createDeclaration(funDecl)
    }

    fun createSecondaryConstructor(decl: String): KtSecondaryConstructor {
        return createClass("class Foo {\n $decl \n}").getSecondaryConstructors().first()
    }

    fun createModifierList(modifier: KtModifierKeywordToken): KtModifierList {
        return createModifierList(modifier.value)
    }

    fun createModifierList(text: String): KtModifierList {
        return createProperty(text + " val x").getModifierList()!!
    }

    fun createModifier(modifier: KtModifierKeywordToken): PsiElement {
        return createModifierList(modifier.value).getModifier(modifier)!!
    }

    fun createAnnotationEntry(text: String): KtAnnotationEntry {
        val modifierList = createProperty(text + " val x").getModifierList()
        return modifierList!!.getAnnotationEntries().first()
    }

    fun createEmptyBody(): KtBlockExpression {
        return createFunction("fun foo() {}").getBodyExpression() as KtBlockExpression
    }

    fun createAnonymousInitializer(): KtAnonymousInitializer {
        return createClass("class A { init {} }").getAnonymousInitializers().first()
    }

    fun createEmptyClassBody(): KtClassBody {
        return createClass("class A(){}").getBody()!!
    }

    fun createParameter(text : String): KtParameter {
        return createClass("class A($text)").getPrimaryConstructorParameters().first()
    }

    fun createParameterList(text: String): KtParameterList {
        return createFunction("fun foo$text{}").getValueParameterList()!!
    }

    fun createFunctionLiteralParameterList(text: String): KtParameterList {
        return (createExpression("{ $text -> 0}") as KtLambdaExpression).functionLiteral.valueParameterList!!
    }

    fun createEnumEntry(text: String): KtEnumEntry {
        return createDeclaration<KtClass>("enum class E {$text}").getDeclarations()[0] as KtEnumEntry
    }

    fun createEnumEntryInitializerList(): KtInitializerList {
        return createEnumEntry("Entry()").initializerList!!
    }

    fun createWhenEntry(entryText: String): KtWhenEntry {
        val function = createFunction("fun foo() { when(12) { " + entryText + " } }")
        val whenEntry = PsiTreeUtil.findChildOfType(function, KtWhenEntry::class.java)

        assert(whenEntry != null) { "Couldn't generate when entry" }
        assert(entryText == whenEntry!!.text) { "Generate when entry text differs from the given text" }

        return whenEntry
    }

    fun createBlockStringTemplateEntry(expression: KtExpression): KtStringTemplateEntryWithExpression {
        val stringTemplateExpression = createExpression("\"\${" + expression.text + "}\"") as KtStringTemplateExpression
        return stringTemplateExpression.entries[0] as KtStringTemplateEntryWithExpression
    }

    fun createSimpleNameStringTemplateEntry(name: String): KtStringTemplateEntryWithExpression {
        val stringTemplateExpression = createExpression("\"\$$name\"") as KtStringTemplateExpression
        return stringTemplateExpression.entries[0] as KtStringTemplateEntryWithExpression
    }

    fun createStringTemplate(content: String) = createExpression("\"$content\"") as KtStringTemplateExpression

    fun createPackageDirective(fqName: FqName): KtPackageDirective {
        return createFile("package ${fqName.asString()}").packageDirective!!
    }

    fun createPackageDirectiveIfNeeded(fqName: FqName): KtPackageDirective? {
        return if (fqName.isRoot) null else createPackageDirective(fqName)
    }

    fun createImportDirective(importPath: ImportPath): KtImportDirective {
        if (importPath.fqnPart().isRoot) {
            throw IllegalArgumentException("import path must not be empty")
        }

        val importDirectiveBuilder = StringBuilder("import ")
        importDirectiveBuilder.append(importPath.pathStr)

        val alias = importPath.alias
        if (alias != null) {
            importDirectiveBuilder.append(" as ").append(alias.asString())
        }

        val file = createFile(importDirectiveBuilder.toString())
        return file.importDirectives.first()
    }

    fun createImportDirectiveWithImportList(importPath: ImportPath): KtImportList {
        val importDirective = createImportDirective(importPath)
        return importDirective.getParent() as KtImportList
    }

    fun createPrimaryConstructor(): KtPrimaryConstructor {
        return createClass("class A()").getPrimaryConstructor()!!
    }

    fun createConstructorKeyword(): PsiElement =
            createClass("class A constructor()").getPrimaryConstructor()!!.getConstructorKeyword()!!

    fun createLabeledExpression(labelName: String): KtLabeledExpression
        = createExpression("$labelName@ 1") as KtLabeledExpression

    fun createFieldIdentifier(fieldName: String): PsiElement {
        return (createExpression("$" + fieldName) as KtNameReferenceExpression).getReferencedNameElement()
    }

    fun createTypeCodeFragment(text: String, context: PsiElement?): KtTypeCodeFragment {
        return KtTypeCodeFragment(project, "fragment.kt", text, context)
    }

    fun createExpressionCodeFragment(text: String, context: PsiElement?): KtExpressionCodeFragment {
        return KtExpressionCodeFragment(project, "fragment.kt", text, null, context)
    }

    fun createBlockCodeFragment(text: String, context: PsiElement?): KtBlockCodeFragment {
        return KtBlockCodeFragment(project, "fragment.kt", text, null, context)
    }

    fun createIf(condition: KtExpression, thenExpr: KtExpression, elseExpr: KtExpression? = null): KtIfExpression {
        return (if (elseExpr != null)
            createExpressionByPattern("if ($0) $1 else $2", condition, thenExpr, elseExpr) as KtIfExpression
        else
            createExpressionByPattern("if ($0) $1", condition, thenExpr)) as KtIfExpression
    }

    fun createArgument(expression: KtExpression?, name: Name? = null, isSpread: Boolean = false): KtValueArgument {
        val argumentList = buildByPattern({ pattern, args -> createByPattern(pattern, *args) { createCallArguments(it) } }) {
            appendFixedText("(")

            if (name != null) {
                appendName(name)
                appendFixedText("=")
            }

            if (isSpread) {
                appendFixedText("*")
            }

            appendExpression(expression)

            appendFixedText(")")
        }
        return argumentList.arguments.single()
    }

    fun createSuperTypeCallEntry(text: String): KtSuperTypeCallEntry {
        return createClass("class A: $text").getSuperTypeListEntries().first() as KtSuperTypeCallEntry
    }

    fun createSuperTypeEntry(text: String): KtSuperTypeEntry {
        return createClass("class A: $text").getSuperTypeListEntries().first() as KtSuperTypeEntry
    }

    fun creareDelegatedSuperTypeEntry(text: String): KtConstructorDelegationCall {
        val colonOrEmpty = if (text.isEmpty()) "" else ": "
        return createClass("class A { constructor()$colonOrEmpty$text {}").getSecondaryConstructors().first().getDelegationCall()
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

        fun modifier(modifier: String): ClassHeaderBuilder {
            assert(state == State.MODIFIERS)

            sb.append(modifier)

            return this
        }

        private fun placeKeyword() {
            assert(state == State.MODIFIERS)

            if (sb.length != 0) {
                sb.append(" ")
            }
            sb.append("class ")

            state = State.NAME
        }


        fun name(name: String): ClassHeaderBuilder {
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

        fun baseClass(name: String, typeArguments: Collection<String>, isInterface: Boolean): ClassHeaderBuilder {
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
        enum class Target {
            FUNCTION,
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
            if (target == Target.FUNCTION) {
                assert(state == State.FIRST_PARAM || state == State.REST_PARAMS)
                sb.append(")")
            }

            state = State.TYPE_CONSTRAINTS
        }

        private fun placeKeyword() {
            assert(state == State.MODIFIERS)

            if (sb.length != 0) {
                sb.append(" ")
            }
            val keyword = when (target) {
                Target.FUNCTION -> "fun"
                Target.READ_ONLY_PROPERTY -> "val"
            }
            sb.append("$keyword ")

            state = State.RECEIVER
        }

        private fun bodyPrefix() = when (target) {
            Target.FUNCTION -> ""
            Target.READ_ONLY_PROPERTY -> "\nget()"
        }

        fun modifier(modifier: String): CallableBuilder {
            assert(state == State.MODIFIERS)

            sb.append(modifier)

            return this
        }

        fun typeParams(values: Collection<String>): CallableBuilder {
            placeKeyword()
            if (!values.isEmpty()) {
                sb.append(values.joinToString(", ", "<", "> ", -1, ""))
            }

            return this
        }

        fun receiver(receiverType: String): CallableBuilder {
            assert(state == State.RECEIVER)

            sb.append(receiverType).append(".")
            state = State.NAME

            return this
        }

        fun name(name: String): CallableBuilder {
            assert(state == State.NAME || state == State.RECEIVER)

            sb.append(name)
            when (target) {
                Target.FUNCTION -> {
                    sb.append("(")
                    state = State.FIRST_PARAM
                }
                else ->
                    state = State.TYPE_CONSTRAINTS
            }

            return this
        }

        fun param(name: String, type: String): CallableBuilder {
            assert(target == Target.FUNCTION)
            assert(state == State.FIRST_PARAM || state == State.REST_PARAMS)

            if (state == State.REST_PARAMS) {
                sb.append(", ")
            }
            sb.append(name).append(": ").append(type)
            if (state == State.FIRST_PARAM) {
                state = State.REST_PARAMS
            }

            return this
        }

        fun returnType(type: String): CallableBuilder {
            closeParams()
            sb.append(": ").append(type)

            return this
        }

        fun noReturnType(): CallableBuilder {
            closeParams()

            return this
        }

        fun typeConstraints(values: Collection<String>): CallableBuilder {
            assert(state == State.TYPE_CONSTRAINTS)

            if (!values.isEmpty()) {
                sb.append(values.joinToString(", ", " where ", "", -1, ""))
            }
            state = State.BODY

            return this
        }

        fun blockBody(body: String): CallableBuilder {
            assert(state == State.BODY || state == State.TYPE_CONSTRAINTS)

            sb.append(bodyPrefix()).append(" {\n").append(body).append("\n}")
            state = State.DONE

            return this
        }

        fun initializer(body: String): CallableBuilder {
            assert(target == Target.READ_ONLY_PROPERTY && (state == State.BODY || state == State.TYPE_CONSTRAINTS))

            sb.append(" = ").append(body)
            state = State.DONE

            return this
        }

        fun lazyBody(body: String): CallableBuilder {
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

    fun createBlock(bodyText: String): KtBlockExpression {
        return createFunction("fun foo() {\n" + bodyText + "\n}").getBodyExpression() as KtBlockExpression
    }

    fun createSingleStatementBlock(statement: KtExpression): KtBlockExpression {
        return createDeclarationByPattern<KtNamedFunction>("fun foo() {\n$0\n}", statement).getBodyExpression() as KtBlockExpression
    }

    fun createComment(text: String): PsiComment {
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
        val block = function.getBodyExpression() as KtBlockExpression
        return BlockWrapper(block, expression)
    }

    private class BlockWrapper(fakeBlockExpression: KtBlockExpression, private val expression: KtExpression) : KtBlockExpression(fakeBlockExpression.node), KtPsiUtil.KtExpressionWrapper {
        override fun getStatements(): List<KtExpression> {
            return listOf(expression)
        }

        override fun getBaseExpression(): KtExpression {
            return expression
        }
    }
}
