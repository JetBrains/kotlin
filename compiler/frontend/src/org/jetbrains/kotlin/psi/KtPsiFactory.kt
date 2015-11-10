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
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.LocalTimeCounter
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtPsiFactory.CallableBuilder.Target
import org.jetbrains.kotlin.resolve.ImportPath

public fun KtPsiFactory(project: Project?): KtPsiFactory = KtPsiFactory(project!!)
public fun KtPsiFactory(elementForProject: PsiElement): KtPsiFactory = KtPsiFactory(elementForProject.getProject())

public var KtFile.doNotAnalyze: String? by UserDataProperty(Key.create("DO_NOT_ANALYZE"))
public var KtFile.analysisContext: PsiElement? by UserDataProperty(Key.create("ANALYSIS_CONTEXT"))
public var KtFile.moduleInfo: ModuleInfo? by UserDataProperty(Key.create("MODULE_INFO"))

public class KtPsiFactory(private val project: Project) {

    public fun createValKeyword(): PsiElement {
        val property = createProperty("val x = 1")
        return property.getValOrVarKeyword()
    }

    public fun createVarKeyword(): PsiElement {
        val property = createProperty("var x = 1")
        return property.getValOrVarKeyword()
    }

    public fun createSafeCallNode(): ASTNode {
        return (createExpression("a?.b") as KtSafeQualifiedExpression).getOperationTokenNode()
    }

    public fun createExpression(text: String): KtExpression {
        //TODO: '\n' below if important - some strange code indenting problems appear without it
        val expression = createProperty("val x =\n$text").getInitializer() ?: error("Failed to create expression from text: '$text'")
        assert(expression.getText() == text) {
            "Failed to create expression from text: '$text', resulting expression's text was: '${expression.getText()}'"
        }
        return expression
    }

    public fun createClassLiteral(className: String): KtClassLiteralExpression =
            createExpression("$className::class") as KtClassLiteralExpression

    public fun createCallArguments(text: String): KtValueArgumentList {
        val property = createProperty("val x = foo $text")
        return (property.getInitializer() as KtCallExpression).getValueArgumentList()!!
    }

    public fun createTypeArguments(text: String): KtTypeArgumentList {
        val property = createProperty("val x = foo$text()")
        return (property.getInitializer() as KtCallExpression).getTypeArgumentList()!!
    }

    public fun createType(type: String): KtTypeReference {
        val typeReference = createProperty("val x : $type").typeReference
        if (typeReference == null || typeReference.text != type) {
            throw IllegalArgumentException("Incorrect type: $type")
        }
        return typeReference
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
        val functionType = createType("() -> Int").typeElement as KtFunctionType
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

    public fun createClass(text: String): KtClass {
        return createDeclaration(text)
    }

    public fun createCompanionObject(): KtObjectDeclaration {
        return createClass("class A {\n companion object{\n}\n}").getCompanionObjects().first()
    }

    public fun createFile(text: String): KtFile {
        return createFile("dummy.kt", text)
    }

    private fun doCreateFile(fileName: String, text: String): KtFile {
        return PsiFileFactory.getInstance(project).createFileFromText(fileName, KotlinFileType.INSTANCE, text, LocalTimeCounter.currentTime(), false) as KtFile
    }

    public fun createFile(fileName: String, text: String): KtFile {
        val file = doCreateFile(fileName, text)

        file.doNotAnalyze = "This file was created by KtPsiFactory and should not be analyzed\n" +
                            "Use createAnalyzableFile to create file that can be analyzed\n"

        return file
    }

    public fun createAnalyzableFile(fileName: String, text: String, contextToAnalyzeIn: PsiElement): KtFile {
        val file = doCreateFile(fileName, text)
        file.analysisContext = contextToAnalyzeIn
        return file
    }

    public fun createPhysicalFile(fileName: String, text: String): KtFile {
        return PsiFileFactory.getInstance(project).createFileFromText(fileName, KotlinFileType.INSTANCE, text, LocalTimeCounter.currentTime(), true) as KtFile
    }

    public fun createProperty(name: String, type: String?, isVar: Boolean, initializer: String?): KtProperty {
        val text = (if (isVar) "var " else "val ") + name + (if (type != null) ":" + type else "") + (if (initializer == null) "" else " = " + initializer)
        return createProperty(text)
    }

    public fun createProperty(name: String, type: String?, isVar: Boolean): KtProperty {
        return createProperty(name, type, isVar, null)
    }

    public fun createProperty(text: String): KtProperty {
        return createDeclaration(text)
    }

    public fun <TDeclaration : KtDeclaration> createDeclaration(text: String): TDeclaration {
        val file = createFile(text)
        val declarations = file.getDeclarations()
        assert(declarations.size() == 1) { "${declarations.size()} declarations in $text" }
        @Suppress("UNCHECKED_CAST")
        val result = declarations.first() as TDeclaration
        return result
    }

    public fun createNameIdentifier(name: String): PsiElement {
        return createProperty(name, null, false).getNameIdentifier()!!
    }

    public fun createSimpleName(name: String): KtSimpleNameExpression {
        return createProperty(name, null, false, name).getInitializer() as KtSimpleNameExpression
    }

    public fun createOperationName(name: String): KtSimpleNameExpression {
        return (createExpression("0 $name 0") as KtBinaryExpression).getOperationReference()
    }

    public fun createIdentifier(name: String): PsiElement {
        return createSimpleName(name).getIdentifier()!!
    }

    public fun createFunction(funDecl: String): KtNamedFunction {
        return createDeclaration(funDecl)
    }

    public fun createSecondaryConstructor(decl: String): KtSecondaryConstructor {
        return createClass("class Foo {\n $decl \n}").getSecondaryConstructors().first()
    }

    public fun createModifierList(modifier: KtModifierKeywordToken): KtModifierList {
        return createModifierList(modifier.getValue())
    }

    public fun createModifierList(text: String): KtModifierList {
        return createProperty(text + " val x").getModifierList()!!
    }

    public fun createModifier(modifier: KtModifierKeywordToken): PsiElement {
        return createModifierList(modifier.getValue()).getModifier(modifier)!!
    }

    public fun createAnnotationEntry(text: String): KtAnnotationEntry {
        val modifierList = createProperty(text + " val x").getModifierList()
        return modifierList!!.getAnnotationEntries().first()
    }

    public fun createEmptyBody(): KtBlockExpression {
        return createFunction("fun foo() {}").getBodyExpression() as KtBlockExpression
    }

    public fun createAnonymousInitializer(): KtClassInitializer {
        return createClass("class A { init {} }").getAnonymousInitializers().first()
    }

    public fun createEmptyClassBody(): KtClassBody {
        return createClass("class A(){}").getBody()!!
    }

    public fun createParameter(text : String): KtParameter {
        return createClass("class A($text)").getPrimaryConstructorParameters().first()
    }

    public fun createParameterList(text: String): KtParameterList {
        return createFunction("fun foo$text{}").getValueParameterList()!!
    }

    public fun createFunctionLiteralParameterList(text: String): KtParameterList {
        return (createExpression("{ $text -> 0}") as KtFunctionLiteralExpression).getFunctionLiteral().getValueParameterList()!!
    }

    public fun createEnumEntry(text: String): KtEnumEntry {
        return createDeclaration<KtClass>("enum class E {$text}").getDeclarations()[0] as KtEnumEntry
    }

    public fun createEnumEntryInitializerList(): KtInitializerList {
        return createEnumEntry("Entry()").initializerList!!
    }

    public fun createWhenEntry(entryText: String): KtWhenEntry {
        val function = createFunction("fun foo() { when(12) { " + entryText + " } }")
        val whenEntry = PsiTreeUtil.findChildOfType(function, javaClass<KtWhenEntry>())

        assert(whenEntry != null) { "Couldn't generate when entry" }
        assert(entryText == whenEntry!!.text) { "Generate when entry text differs from the given text" }

        return whenEntry
    }

    public fun createBlockStringTemplateEntry(expression: KtExpression): KtStringTemplateEntryWithExpression {
        val stringTemplateExpression = createExpression("\"\${" + expression.getText() + "}\"") as KtStringTemplateExpression
        return stringTemplateExpression.getEntries()[0] as KtStringTemplateEntryWithExpression
    }

    public fun createSimpleNameStringTemplateEntry(name: String): KtStringTemplateEntryWithExpression {
        val stringTemplateExpression = createExpression("\"\$$name\"") as KtStringTemplateExpression
        return stringTemplateExpression.getEntries()[0] as KtStringTemplateEntryWithExpression
    }

    public fun createPackageDirective(fqName: FqName): KtPackageDirective {
        return createFile("package ${fqName.asString()}").getPackageDirective()!!
    }

    public fun createPackageDirectiveIfNeeded(fqName: FqName): KtPackageDirective? {
        return if (fqName.isRoot()) null else createPackageDirective(fqName)
    }

    public fun createImportDirective(importPath: ImportPath): KtImportDirective {
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

    public fun createImportDirectiveWithImportList(importPath: ImportPath): KtImportList {
        val importDirective = createImportDirective(importPath)
        return importDirective.getParent() as KtImportList
    }

    public fun createPrimaryConstructor(): KtPrimaryConstructor {
        return createClass("class A()").getPrimaryConstructor()!!
    }

    public fun createConstructorKeyword(): PsiElement =
            createClass("class A constructor()").getPrimaryConstructor()!!.getConstructorKeyword()!!

    public fun createLabeledExpression(labelName: String): KtLabeledExpression
        = createExpression("$labelName@ 1") as KtLabeledExpression

    public fun createFieldIdentifier(fieldName: String): PsiElement {
        return (createExpression("$" + fieldName) as KtNameReferenceExpression).getReferencedNameElement()
    }

    public fun createTypeCodeFragment(text: String, context: PsiElement?): KtTypeCodeFragment {
        return KtTypeCodeFragment(project, "fragment.kt", text, context)
    }

    public fun createExpressionCodeFragment(text: String, context: PsiElement?): KtExpressionCodeFragment {
        return KtExpressionCodeFragment(project, "fragment.kt", text, null, context)
    }

    public fun createBlockCodeFragment(text: String, context: PsiElement?): KtBlockCodeFragment {
        return KtBlockCodeFragment(project, "fragment.kt", text, null, context)
    }

    public fun createIf(condition: KtExpression, thenExpr: KtExpression, elseExpr: KtExpression? = null): KtIfExpression {
        return (if (elseExpr != null)
            createExpressionByPattern("if ($0) $1 else $2", condition, thenExpr, elseExpr) as KtIfExpression
        else
            createExpressionByPattern("if ($0) $1", condition, thenExpr)) as KtIfExpression
    }

    public fun createArgument(expression: KtExpression?, name: Name? = null, isSpread: Boolean = false): KtValueArgument {
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

    public fun createDelegatorToSuperCall(text: String): KtDelegatorToSuperCall {
        return createClass("class A: $text").getDelegationSpecifiers().first() as KtDelegatorToSuperCall
    }

    public fun createDelegatorToSuperClass(text: String): KtDelegatorToSuperClass {
        return createClass("class A: $text").getDelegationSpecifiers().first() as KtDelegatorToSuperClass
    }

    public fun createConstructorDelegationCall(text: String): KtConstructorDelegationCall {
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

        public fun initializer(body: String): CallableBuilder {
            assert(target == Target.READ_ONLY_PROPERTY && (state == State.BODY || state == State.TYPE_CONSTRAINTS))

            sb.append(" = ").append(body)
            state = State.DONE

            return this
        }

        public fun lazyBody(body: String): CallableBuilder {
            assert(target == Target.READ_ONLY_PROPERTY && (state == State.BODY || state == State.TYPE_CONSTRAINTS))

            sb.append(" by kotlin.lazy {\n").append(body).append("\n}")
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

    public fun createBlock(bodyText: String): KtBlockExpression {
        return createFunction("fun foo() {\n" + bodyText + "\n}").getBodyExpression() as KtBlockExpression
    }

    public fun createSingleStatementBlock(statement: KtExpression): KtBlockExpression {
        return createDeclarationByPattern<KtNamedFunction>("fun foo() {\n$0\n}", statement).getBodyExpression() as KtBlockExpression
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
    public fun wrapInABlockWrapper(expression: KtExpression): KtBlockExpression {
        if (expression is KtBlockExpression) {
            return expression
        }
        val function = createFunction("fun f() { ${expression.getText()} }")
        val block = function.getBodyExpression() as KtBlockExpression
        return BlockWrapper(block, expression)
    }

    private class BlockWrapper(fakeBlockExpression: KtBlockExpression, private val expression: KtExpression) : KtBlockExpression(fakeBlockExpression.getNode()), KtPsiUtil.KtExpressionWrapper {
        override fun getStatements(): List<KtExpression> {
            return listOf(expression)
        }

        override fun getBaseExpression(): KtExpression {
            return expression
        }
    }
}
