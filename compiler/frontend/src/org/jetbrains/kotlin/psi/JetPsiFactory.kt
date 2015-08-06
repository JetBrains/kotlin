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

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.LocalTimeCounter
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.idea.JetFileType
import org.jetbrains.kotlin.lexer.JetKeywordToken
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetPsiFactory.CallableBuilder.Target
import org.jetbrains.kotlin.resolve.ImportPath
import java.io.PrintWriter
import java.io.StringWriter

public fun JetPsiFactory(project: Project?): JetPsiFactory = JetPsiFactory(project!!)
public fun JetPsiFactory(elementForProject: PsiElement): JetPsiFactory = JetPsiFactory(elementForProject.getProject())

public var JetFile.doNotAnalyze: String? by UserDataProperty(Key.create("DO_NOT_ANALYZE"))
public var JetFile.analysisContext: PsiElement? by UserDataProperty(Key.create("ANALYSIS_CONTEXT"))
public var JetFile.moduleInfo: ModuleInfo? by UserDataProperty(Key.create("MODULE_INFO"))

public class JetPsiFactory(private val project: Project) {

    public fun createValKeyword(): PsiElement {
        val property = createProperty("val x = 1")
        return property.getValOrVarKeyword()
    }

    public fun createVarKeyword(): PsiElement {
        val property = createProperty("var x = 1")
        return property.getValOrVarKeyword()
    }

    public fun createSafeCallNode(): ASTNode {
        return (createExpression("a?.b") as JetSafeQualifiedExpression).getOperationTokenNode()
    }

    public fun createExpression(text: String): JetExpression {
        //TODO: '\n' below if important - some strange code indenting problems appear without it
        val expression = createProperty("val x =\n$text").getInitializer() ?: error("Failed to create expression from text: '$text'")
        assert(expression.getText() == text) {
            "Failed to create expression from text: '$text', resulting expression's text was: '${expression.getText()}'"
        }
        return expression
    }

    public fun createClassLiteral(className: String): JetClassLiteralExpression =
            createExpression("$className::class") as JetClassLiteralExpression

    public fun createCallArguments(text: String): JetValueArgumentList {
        val property = createProperty("val x = foo $text")
        return (property.getInitializer() as JetCallExpression).getValueArgumentList()!!
    }

    public fun createTypeArguments(text: String): JetTypeArgumentList {
        val property = createProperty("val x = foo$text()")
        return (property.getInitializer() as JetCallExpression).getTypeArgumentList()!!
    }

    public fun createType(type: String): JetTypeReference {
        return createProperty("val x : $type").getTypeReference()!!
    }

    public fun createStar(): PsiElement {
        return createType("List<*>").findElementAt(5)!!
    }

    public fun createComma(): PsiElement {
        return createType("T<X, Y>").findElementAt(3)!!
    }

    public fun createDot(): PsiElement {
        return createType("T.(X)").findElementAt(1)!!
    }

    public fun createColon(): PsiElement {
        return createProperty("val x: Int").findElementAt(5)!!
    }

    public fun createEQ(): PsiElement {
        return createFunction("fun foo() = foo").getEqualsToken()!!
    }

    public fun createSemicolon(): PsiElement {
        return createProperty("val x: Int;").findElementAt(10)!!
    }

    //the pair contains the first and the last elements of a range
    public fun createWhitespaceAndArrow(): Pair<PsiElement, PsiElement> {
        val functionType = createType("() -> Int").getTypeElement() as JetFunctionType
        return Pair(functionType.findElementAt(2)!!, functionType.findElementAt(3)!!)
    }

    public fun createWhiteSpace(): PsiElement {
        return createWhiteSpace(" ")
    }

    public fun createWhiteSpace(text: String): PsiElement {
        return createProperty("val${text}x").findElementAt(3)!!
    }

    // Remove when all Java usages are rewritten to Kotlin
    public fun createNewLine(): PsiElement {
        return createWhiteSpace("\n")
    }

    public fun createNewLine(lineBreaks: Int): PsiElement {
        return createWhiteSpace("\n".repeat(lineBreaks))
    }

    public fun createClass(text: String): JetClass {
        return createDeclaration(text)
    }

    public fun createCompanionObject(): JetObjectDeclaration {
        return createClass("class A {\n companion object{\n}\n}").getCompanionObjects().first()
    }

    public fun createFile(text: String): JetFile {
        return createFile("dummy.kt", text)
    }

    private fun doCreateFile(fileName: String, text: String): JetFile {
        return PsiFileFactory.getInstance(project).createFileFromText(fileName, JetFileType.INSTANCE, text, LocalTimeCounter.currentTime(), false) as JetFile
    }

    public fun createFile(fileName: String, text: String): JetFile {
        val file = doCreateFile(fileName, text)

        file.doNotAnalyze = "This file was created by JetPsiFactory and should not be analyzed\n" +
                            "Use createAnalyzableFile to create file that can be analyzed\n"

        return file
    }

    public fun createAnalyzableFile(fileName: String, text: String, contextToAnalyzeIn: PsiElement): JetFile {
        val file = doCreateFile(fileName, text)
        file.analysisContext = contextToAnalyzeIn
        return file
    }

    public fun createPhysicalFile(fileName: String, text: String): JetFile {
        return PsiFileFactory.getInstance(project).createFileFromText(fileName, JetFileType.INSTANCE, text, LocalTimeCounter.currentTime(), true) as JetFile
    }

    public fun createProperty(name: String, type: String?, isVar: Boolean, initializer: String?): JetProperty {
        val text = (if (isVar) "var " else "val ") + name + (if (type != null) ":" + type else "") + (if (initializer == null) "" else " = " + initializer)
        return createProperty(text)
    }

    public fun createProperty(name: String, type: String?, isVar: Boolean): JetProperty {
        return createProperty(name, type, isVar, null)
    }

    public fun createProperty(text: String): JetProperty {
        return createDeclaration(text)
    }

    public fun <TDeclaration : JetDeclaration> createDeclaration(text: String): TDeclaration {
        val file = createFile(text)
        val declarations = file.getDeclarations()
        assert(declarations.size() == 1) { "${declarations.size()} declarations in $text" }
        @suppress("UNCHECKED_CAST")
        val result = declarations.first() as TDeclaration
        return result
    }

    public fun createNameIdentifier(name: String): PsiElement {
        return createProperty(name, null, false).getNameIdentifier()!!
    }

    public fun createSimpleName(name: String): JetSimpleNameExpression {
        return createProperty(name, null, false, name).getInitializer() as JetSimpleNameExpression
    }

    public fun createOperationName(name: String): JetSimpleNameExpression {
        return (createExpression("0 $name 0") as JetBinaryExpression).getOperationReference()
    }

    public fun createObjectDeclarationName(name: String): JetObjectDeclarationName {
        return createDeclaration<JetObjectDeclaration>("object $name").getNameAsDeclaration()!!
    }

    public fun createIdentifier(name: String): PsiElement {
        return createSimpleName(name).getIdentifier()!!
    }

    public fun createFunction(funDecl: String): JetNamedFunction {
        return createDeclaration(funDecl)
    }

    public fun createSecondaryConstructor(decl: String): JetSecondaryConstructor {
        return createClass("class Foo {\n $decl \n}").getSecondaryConstructors().first()
    }

    public fun createModifierList(modifier: JetModifierKeywordToken): JetModifierList {
        return createModifierList(modifier.getValue())
    }

    public fun createModifierList(text: String): JetModifierList {
        return createProperty(text + " val x").getModifierList()!!
    }

    public fun createModifier(modifier: JetModifierKeywordToken): PsiElement {
        return createModifierList(modifier.getValue()).getModifier(modifier)!!
    }

    public fun createAnnotationEntry(text: String): JetAnnotationEntry {
        val modifierList = createProperty(text + " val x").getModifierList()
        return modifierList!!.getAnnotationEntries().first()
    }

    public fun createEmptyBody(): JetBlockExpression {
        return createFunction("fun foo() {}").getBodyExpression() as JetBlockExpression
    }

    public fun createAnonymousInitializer(): JetClassInitializer {
        return createClass("class A { init {} }").getAnonymousInitializers().first()
    }

    public fun createEmptyClassBody(): JetClassBody {
        return createClass("class A(){}").getBody()!!
    }

    public fun createParameter(text : String): JetParameter {
        return createClass("class A($text)").getPrimaryConstructorParameters().first()
    }

    public fun createParameterList(text: String): JetParameterList {
        return createFunction("fun foo$text{}").getValueParameterList()!!
    }

    public fun createFunctionLiteralParameterList(text: String): JetParameterList {
        return (createExpression("{ $text -> 0}") as JetFunctionLiteralExpression).getFunctionLiteral().getValueParameterList()!!
    }

    public fun createEnumEntry(text: String): JetEnumEntry {
        return createDeclaration<JetClass>("enum class E {$text}").getDeclarations()[0] as JetEnumEntry
    }

    public fun createEnumEntrySuperclassReferenceExpression(): JetEnumEntrySuperclassReferenceExpression {
        val userType = createEnumEntry("Entry()").getInitializerList()!!.getInitializers()[0].getTypeReference()!!.getTypeElement() as JetUserType
        return userType.getReferenceExpression() as JetEnumEntrySuperclassReferenceExpression
    }

    public fun createWhenEntry(entryText: String): JetWhenEntry {
        val function = createFunction("fun foo() { when(12) { " + entryText + " } }")
        val whenEntry = PsiTreeUtil.findChildOfType(function, javaClass<JetWhenEntry>())

        assert(whenEntry != null, "Couldn't generate when entry")
        assert(entryText == whenEntry!!.getText(), "Generate when entry text differs from the given text")

        return whenEntry
    }

    public fun createBlockStringTemplateEntry(expression: JetExpression): JetStringTemplateEntryWithExpression {
        val stringTemplateExpression = createExpression("\"\${" + expression.getText() + "}\"") as JetStringTemplateExpression
        return stringTemplateExpression.getEntries()[0] as JetStringTemplateEntryWithExpression
    }

    public fun createSimpleNameStringTemplateEntry(name: String): JetStringTemplateEntryWithExpression {
        val stringTemplateExpression = createExpression("\"\$$name\"") as JetStringTemplateExpression
        return stringTemplateExpression.getEntries()[0] as JetStringTemplateEntryWithExpression
    }

    public fun createPackageDirective(fqName: FqName): JetPackageDirective {
        return createFile("package ${fqName.asString()}").getPackageDirective()!!
    }

    public fun createPackageDirectiveIfNeeded(fqName: FqName): JetPackageDirective? {
        return if (fqName.isRoot()) null else createPackageDirective(fqName)
    }

    public fun createImportDirective(path: String): JetImportDirective {
        return createImportDirective(ImportPath(path))
    }

    public fun createImportDirective(importPath: ImportPath): JetImportDirective {
        if (importPath.fqnPart().isRoot()) {
            throw IllegalArgumentException("import path must not be empty")
        }

        val importDirectiveBuilder = StringBuilder("import ")
        importDirectiveBuilder.append(importPath.getPathStr())

        val alias = importPath.getAlias()
        if (alias != null) {
            importDirectiveBuilder.append(" as ").append(alias.asString())
        }

        val file = createFile(importDirectiveBuilder.toString())
        return file.getImportDirectives().first()
    }

    public fun createImportDirectiveWithImportList(importPath: ImportPath): JetImportList {
        val importDirective = createImportDirective(importPath)
        return importDirective.getParent() as JetImportList
    }

    public fun createPrimaryConstructor(): JetPrimaryConstructor {
        return createClass("class A()").getPrimaryConstructor()!!
    }

    public fun createConstructorKeyword(): PsiElement =
            createClass("class A constructor()").getPrimaryConstructor()!!.getConstructorKeyword()!!

    public fun createClassLabel(labelName: String): JetSimpleNameExpression {
        return (createExpression("this@" + labelName) as JetThisExpression).getTargetLabel()!!
    }

    public fun createLabeledExpression(labelName: String): JetLabeledExpression
        = createExpression("$labelName@ 1") as JetLabeledExpression

    public fun createFieldIdentifier(fieldName: String): PsiElement {
        return (createExpression("$" + fieldName) as JetNameReferenceExpression).getReferencedNameElement()
    }

    public fun createTypeCodeFragment(text: String, context: PsiElement?): JetTypeCodeFragment {
        return JetTypeCodeFragment(project, "fragment.kt", text, context)
    }

    public fun createExpressionCodeFragment(text: String, context: PsiElement?): JetExpressionCodeFragment {
        return JetExpressionCodeFragment(project, "fragment.kt", text, null, context)
    }

    public fun createBlockCodeFragment(text: String, context: PsiElement?): JetBlockCodeFragment {
        return JetBlockCodeFragment(project, "fragment.kt", text, null, context)
    }

    public fun createIf(condition: JetExpression, thenExpr: JetExpression, elseExpr: JetExpression? = null): JetIfExpression {
        return (if (elseExpr != null)
            createExpressionByPattern("if ($0) $1 else $2", condition, thenExpr, elseExpr) as JetIfExpression
        else
            createExpressionByPattern("if ($0) $1", condition, thenExpr)) as JetIfExpression
    }

    public fun createArgument(expression: JetExpression, name: Name? = null, isSpread: Boolean = false): JetValueArgument {
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
        return argumentList.getArguments().single()
    }

    public fun createDelegatorToSuperCall(text: String): JetDelegatorToSuperCall {
        return createClass("class A: $text").getDelegationSpecifiers().first() as JetDelegatorToSuperCall
    }

    public fun createDelegatorToSuperClass(text: String): JetDelegatorToSuperClass {
        return createClass("class A: $text").getDelegationSpecifiers().first() as JetDelegatorToSuperClass
    }

    public fun createConstructorDelegationCall(text: String): JetConstructorDelegationCall {
        val colonOrEmpty = if (text.isEmpty()) "" else ": "
        return createClass("class A { constructor()$colonOrEmpty$text {}").getSecondaryConstructors().first().getDelegationCall()
    }

    public class CallableBuilder(private val target: Target) {
        public enum class Target {
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

            if (sb.length() != 0) {
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

        public fun modifier(modifier: String): CallableBuilder {
            assert(state == State.MODIFIERS)

            sb.append(modifier)

            return this
        }

        public fun typeParams(values: Collection<String>): CallableBuilder {
            placeKeyword()
            if (!values.isEmpty()) {
                sb.append(values.joinToString(", ", "<", "> ", -1, ""))
            }

            return this
        }

        public fun receiver(receiverType: String): CallableBuilder {
            assert(state == State.RECEIVER)

            sb.append(receiverType).append(".")
            state = State.NAME

            return this
        }

        public fun name(name: String): CallableBuilder {
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

        public fun param(name: String, type: String): CallableBuilder {
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

        public fun returnType(type: String): CallableBuilder {
            closeParams()
            sb.append(": ").append(type)

            return this
        }

        public fun noReturnType(): CallableBuilder {
            closeParams()

            return this
        }

        public fun typeConstraints(values: Collection<String>): CallableBuilder {
            assert(state == State.TYPE_CONSTRAINTS)

            if (!values.isEmpty()) {
                sb.append(values.joinToString(", ", " where ", "", -1, ""))
            }
            state = State.BODY

            return this
        }

        public fun blockBody(body: String): CallableBuilder {
            assert(state == State.BODY || state == State.TYPE_CONSTRAINTS)

            sb.append(bodyPrefix()).append(" {\n").append(body).append("\n}")
            state = State.DONE

            return this
        }

        public fun expressionBody(body: String): CallableBuilder {
            assert(state == State.BODY || state == State.TYPE_CONSTRAINTS)

            sb.append(bodyPrefix()).append(" = ").append(body)
            state = State.DONE

            return this
        }

        public fun initializer(body: String): CallableBuilder {
            assert(target == Target.READ_ONLY_PROPERTY && (state == State.BODY || state == State.TYPE_CONSTRAINTS))

            sb.append(" = ").append(body)
            state = State.DONE

            return this
        }

        public fun lazyBody(body: String): CallableBuilder {
            assert(target == Target.READ_ONLY_PROPERTY && (state == State.BODY || state == State.TYPE_CONSTRAINTS))

            sb.append(" by kotlin.properties.Delegates.lazy {\n").append(body).append("\n}")
            state = State.DONE

            return this
        }

        public fun asString(): String {
            if (state != State.DONE) {
                state = State.DONE
            }

            return sb.toString()
        }
    }

    public fun createBlock(bodyText: String): JetBlockExpression {
        return createFunction("fun foo() {\n" + bodyText + "\n}").getBodyExpression() as JetBlockExpression
    }

    public fun createSingleStatementBlock(statement: JetExpression): JetBlockExpression {
        return createDeclarationByPattern<JetNamedFunction>("fun foo() {\n$0\n}", statement).getBodyExpression() as JetBlockExpression
    }

    public fun createComment(text: String): PsiComment {
        val file = createFile(text)
        val comments = file.getChildren().filterIsInstance<PsiComment>()
        val comment = comments.single()
        assert(comment.getText() == text)
        return comment
    }

    // special hack used in ControlStructureTypingVisitor
    // TODO: get rid of it
    public fun wrapInABlockWrapper(expression: JetExpression): JetBlockExpression {
        if (expression is JetBlockExpression) {
            return expression
        }
        val function = createFunction("fun f() { ${expression.getText()} }")
        val block = function.getBodyExpression() as JetBlockExpression
        return BlockWrapper(block, expression)
    }

    private class BlockWrapper(fakeBlockExpression: JetBlockExpression, private val expression: JetExpression) : JetBlockExpression(fakeBlockExpression.getNode()), JetPsiUtil.JetExpressionWrapper {
        override fun getStatements(): List<JetExpression> {
            return listOf(expression)
        }

        override fun getBaseExpression(): JetExpression {
            return expression
        }
    }
}
