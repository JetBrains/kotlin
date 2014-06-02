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

package org.jetbrains.jet.lang.parsing;

import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeType;
import org.jetbrains.jet.lexer.JetKeywordToken;

import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.jet.JetNodeTypes.*;
import static org.jetbrains.jet.lexer.JetTokens.*;

public class JetParsing extends AbstractJetParsing {
    // TODO: token sets to constants, including derived methods
    public static final Map<String, IElementType> MODIFIER_KEYWORD_MAP = new HashMap<String, IElementType>();
    static {
        for (IElementType softKeyword : MODIFIER_KEYWORDS.getTypes()) {
            MODIFIER_KEYWORD_MAP.put(((JetKeywordToken) softKeyword).getValue(), softKeyword);
        }
    }

    private static final TokenSet TOPLEVEL_OBJECT_FIRST = TokenSet.create(TYPE_KEYWORD, TRAIT_KEYWORD, CLASS_KEYWORD,
                FUN_KEYWORD, VAL_KEYWORD, PACKAGE_KEYWORD);
    private static final TokenSet ENUM_MEMBER_FIRST = TokenSet.create(TYPE_KEYWORD, TRAIT_KEYWORD, CLASS_KEYWORD,
                FUN_KEYWORD, VAL_KEYWORD, IDENTIFIER);

    private static final TokenSet CLASS_NAME_RECOVERY_SET = TokenSet.orSet(TokenSet.create(LT, LPAR, COLON, LBRACE), TOPLEVEL_OBJECT_FIRST);
    private static final TokenSet TYPE_PARAMETER_GT_RECOVERY_SET = TokenSet.create(WHERE_KEYWORD, LPAR, COLON, LBRACE, GT);
    private static final TokenSet PARAMETER_NAME_RECOVERY_SET = TokenSet.create(COLON, EQ, COMMA, RPAR);
    private static final TokenSet PACKAGE_NAME_RECOVERY_SET = TokenSet.create(DOT, EOL_OR_SEMICOLON);
    private static final TokenSet IMPORT_RECOVERY_SET = TokenSet.create(AS_KEYWORD, DOT, EOL_OR_SEMICOLON);
    /*package*/ static final TokenSet TYPE_REF_FIRST = TokenSet.create(LBRACKET, IDENTIFIER, LPAR, CAPITALIZED_THIS_KEYWORD, HASH);
    private static final TokenSet RECEIVER_TYPE_TERMINATORS = TokenSet.create(DOT, SAFE_ACCESS);
    private static final TokenSet VALUE_PARAMETER_FIRST =
            TokenSet.orSet(TokenSet.create(IDENTIFIER, LBRACKET, VAL_KEYWORD, VAR_KEYWORD), MODIFIER_KEYWORDS);
    private static final TokenSet LAMBDA_VALUE_PARAMETER_FIRST =
            TokenSet.orSet(TokenSet.create(IDENTIFIER, LBRACKET), MODIFIER_KEYWORDS);

    static JetParsing createForTopLevel(SemanticWhitespaceAwarePsiBuilder builder) {
        JetParsing jetParsing = new JetParsing(builder);
        jetParsing.myExpressionParsing = new JetExpressionParsing(builder, jetParsing);
        return jetParsing;
    }

    private static JetParsing createForByClause(SemanticWhitespaceAwarePsiBuilder builder) {
        final SemanticWhitespaceAwarePsiBuilderForByClause builderForByClause = new SemanticWhitespaceAwarePsiBuilderForByClause(builder);
        JetParsing jetParsing = new JetParsing(builderForByClause);
        jetParsing.myExpressionParsing = new JetExpressionParsing(builderForByClause, jetParsing) {
            @Override
            protected boolean parseCallWithClosure() {
                if (builderForByClause.getStackSize() > 0) {
                    return super.parseCallWithClosure();
                }
                return false;
            }

            @Override
            protected JetParsing create(SemanticWhitespaceAwarePsiBuilder builder) {
                return createForByClause(builder);
            }
        };
        return jetParsing;
    }

    private JetExpressionParsing myExpressionParsing;

    private JetParsing(SemanticWhitespaceAwarePsiBuilder builder) {
        super(builder);
    }

    /*
     * [start] jetlFile
     *   : preamble toplevelObject* [eof]
     *   ;
     */
    void parseFile() {
        PsiBuilder.Marker fileMarker = mark();

        parsePreamble();

        while (!eof()) {
            parseTopLevelObject();
        }

        fileMarker.done(JET_FILE);
    }

    void parseTypeCodeFragment() {
        PsiBuilder.Marker marker = mark();
        parseTypeRef();

        checkForUnexpectedSymbols();

        marker.done(TYPE_CODE_FRAGMENT);
    }

    void parseExpressionCodeFragment() {
        PsiBuilder.Marker marker = mark();
        myExpressionParsing.parseExpression();

        checkForUnexpectedSymbols();

        marker.done(EXPRESSION_CODE_FRAGMENT);
    }

    void parseBlockCodeFragment() {
        PsiBuilder.Marker marker = mark();
        PsiBuilder.Marker blockMarker = mark();

        if (at(PACKAGE_KEYWORD) || at(IMPORT_KEYWORD)) {
            PsiBuilder.Marker err = mark();
            parsePreamble();
            err.error("Package directive and imports are forbidden in code fragments");
        }

        myExpressionParsing.parseStatements();

        checkForUnexpectedSymbols();

        blockMarker.done(BLOCK);
        marker.done(BLOCK_CODE_FRAGMENT);
    }

    void parseScript() {
        PsiBuilder.Marker fileMarker = mark();

        parsePreamble();

        PsiBuilder.Marker scriptMarker = mark();

        PsiBuilder.Marker blockMarker = mark();

        myExpressionParsing.parseStatements();

        checkForUnexpectedSymbols();

        blockMarker.done(BLOCK);
        scriptMarker.done(SCRIPT);
        fileMarker.done(JET_FILE);
    }

    private void checkForUnexpectedSymbols() {
        while (!eof()) {
            errorAndAdvance("unexpected symbol");
        }
    }

    /*
     *preamble
     *  : packageDirective? import*
     *  ;
     */
    private void parsePreamble() {
        /*
         * packageDirective
         *   : modifiers "package" SimpleName{"."} SEMI?
         *   ;
         */
        PsiBuilder.Marker packageDirective = mark();
        PsiBuilder.Marker firstEntry = mark();
        parseModifierList(MODIFIER_LIST, true);

        if (at(PACKAGE_KEYWORD)) {
            advance(); // PACKAGE_KEYWORD


            parsePackageName();

            if (at(LBRACE)) {
                // Because it's blocked package and it will be parsed as one of top level objects
                firstEntry.rollbackTo();
                packageDirective.done(PACKAGE_DIRECTIVE);
                return;
            }

            firstEntry.drop();

            consumeIf(SEMICOLON);
        }
        else {
            firstEntry.rollbackTo();
        }
        packageDirective.done(PACKAGE_DIRECTIVE);

        parseImportDirectives();
    }

    /* SimpleName{"."} */
    private void parsePackageName() {
        PsiBuilder.Marker qualifiedExpression = mark();
        boolean simpleName = true;
        while (true) {
            if (myBuilder.newlineBeforeCurrentToken()) {
                errorWithRecovery("Package name must be a '.'-separated identifier list placed on a single line", PACKAGE_NAME_RECOVERY_SET);
                break;
            }

            PsiBuilder.Marker nsName = mark();
            if (expect(IDENTIFIER, "Package name must be a '.'-separated identifier list", PACKAGE_NAME_RECOVERY_SET)) {
                nsName.done(REFERENCE_EXPRESSION);
            }
            else {
                nsName.drop();
            }

            if (!simpleName) {
                PsiBuilder.Marker precedingMarker = qualifiedExpression.precede();
                qualifiedExpression.done(DOT_QUALIFIED_EXPRESSION);
                qualifiedExpression = precedingMarker;
            }

            if (at(DOT)) {
                simpleName = false;
                advance(); // DOT
            }
            else {
                break;
            }
        }
        qualifiedExpression.drop();
    }

    /*
     * import
     *   : "import" SimpleName{"."} ("." "*" | "as" SimpleName)? SEMI?
     *   ;
     */
    private void parseImportDirective() {
        assert _at(IMPORT_KEYWORD);
        PsiBuilder.Marker importDirective = mark();
        advance(); // IMPORT_KEYWORD

        if (closeImportWithErrorIfNewline(importDirective, "Expecting qualified name")) {
            return;
        }

        PsiBuilder.Marker qualifiedName = mark();

        PsiBuilder.Marker reference = mark();
        expect(IDENTIFIER, "Expecting qualified name");
        reference.done(REFERENCE_EXPRESSION);

        while (at(DOT) && lookahead(1) != MUL) {
            advance(); // DOT

            if (closeImportWithErrorIfNewline(importDirective, "Import must be placed on a single line")) {
                qualifiedName.drop();
                return;
            }

            reference = mark();
            if (expect(IDENTIFIER, "Qualified name must be a '.'-separated identifier list", IMPORT_RECOVERY_SET)) {
                reference.done(REFERENCE_EXPRESSION);
            }
            else {
                reference.drop();
            }

            PsiBuilder.Marker precede = qualifiedName.precede();
            qualifiedName.done(DOT_QUALIFIED_EXPRESSION);
            qualifiedName = precede;
        }
        qualifiedName.drop();

        if (at(DOT)) {
            advance(); // DOT
            assert _at(MUL);
            advance(); // MUL
            if (at(AS_KEYWORD)) {
                PsiBuilder.Marker as = mark();
                advance(); // AS_KEYWORD
                if (closeImportWithErrorIfNewline(importDirective, "Expecting identifier")) {
                    as.drop();
                    return;
                }
                consumeIf(IDENTIFIER);
                as.error("Cannot rename all imported items to one identifier");
            }
        }
        if (at(AS_KEYWORD)) {
            advance(); // AS_KEYWORD
            if (closeImportWithErrorIfNewline(importDirective, "Expecting identifier")) {
                return;
            }
            expect(IDENTIFIER, "Expecting identifier", TokenSet.create(SEMICOLON));
        }
        consumeIf(SEMICOLON);
        importDirective.done(IMPORT_DIRECTIVE);
    }

    private boolean closeImportWithErrorIfNewline(PsiBuilder.Marker importDirective, String errorMessage) {
        if (myBuilder.newlineBeforeCurrentToken()) {
            error(errorMessage);
            importDirective.done(IMPORT_DIRECTIVE);
            return true;
        }
        return false;
    }

    private void parseImportDirectives() {
        if (at(IMPORT_KEYWORD)) {
            PsiBuilder.Marker importList = mark();
            while (at(IMPORT_KEYWORD)) {
                parseImportDirective();
            }
            importList.done(IMPORT_LIST);
        }
    }

    /*
     * toplevelObject
     *   : package
     *   : class
     *   : extension
     *   : function
     *   : property
     *   : typedef
     *   : object
     *   ;
     */
    private void parseTopLevelObject() {
        PsiBuilder.Marker decl = mark();

        TokenDetector detector = new TokenDetector(ENUM_KEYWORD);
        parseModifierList(MODIFIER_LIST, detector, true);

        IElementType keywordToken = tt();
        IElementType declType = null;
//        if (keywordToken == PACKAGE_KEYWORD) {
//            declType = parsePackageBlock();
//        }
//        else
        if (keywordToken == CLASS_KEYWORD || keywordToken == TRAIT_KEYWORD) {
            declType = parseClass(detector.isDetected());
        }
        else if (keywordToken == FUN_KEYWORD) {
            declType = parseFunction();
        }
        else if (keywordToken == VAL_KEYWORD || keywordToken == VAR_KEYWORD) {
            declType = parseProperty();
        }
        else if (keywordToken == TYPE_KEYWORD) {
            declType = parseTypeDef();
        }
        else if (keywordToken == OBJECT_KEYWORD) {
            parseObject(true, true);
            declType = OBJECT_DECLARATION;
        }

        if (declType == null) {
            errorAndAdvance("Expecting package directive or top level declaration");
            decl.drop();
        }
        else {
            decl.done(declType);
        }
    }

    /*
     * (modifier | attribute)*
     */
    boolean parseModifierList(IElementType nodeType, boolean allowShortAnnotations) {
        return parseModifierList(nodeType, null, allowShortAnnotations);
    }

    /**
     * (modifier | attribute)*
     *
     * Feeds modifiers (not attributes) into the passed consumer, if it is not null
     */
    boolean parseModifierList(IElementType nodeType, @Nullable Consumer<IElementType> tokenConsumer, boolean allowShortAnnotations) {
        PsiBuilder.Marker list = mark();
        boolean empty = true;
        while (!eof()) {
            if (atSet(MODIFIER_KEYWORDS)) {
                if (tokenConsumer != null) tokenConsumer.consume(tt());
                advance(); // MODIFIER
            }
            else if (at(LBRACKET) || (allowShortAnnotations && at(IDENTIFIER))) {
                parseAnnotation(allowShortAnnotations);
            }
            else {
                break;
            }
            empty = false;
        }
        if (empty) {
            list.drop();
        }
        else {
            list.done(nodeType);
        }
        return !empty;
    }

    /*
     * annotations
     *   : annotation*
     *   ;
     */
    void parseAnnotations(boolean allowShortAnnotations) {
        while (true) {
            if (!(parseAnnotation(allowShortAnnotations))) break;
        }
    }

    /*
     * annotation
     *   : "[" annotationEntry+ "]"
     *   : annotationEntry
     *   ;
     */
    private boolean parseAnnotation(boolean allowShortAnnotations) {
        if (at(LBRACKET)) {
            PsiBuilder.Marker annotation = mark();

            myBuilder.disableNewlines();
            advance(); // LBRACKET

            if (!at(IDENTIFIER)) {
                error("Expecting a list of attributes");
            }
            else {
                parseAnnotationEntry();
                while (at(COMMA)) {
                    errorAndAdvance("No commas needed to separate attributes");
                }

                while (at(IDENTIFIER)) {
                    parseAnnotationEntry();
                    while (at(COMMA)) {
                        errorAndAdvance("No commas needed to separate attributes");
                    }
                }
            }

            expect(RBRACKET, "Expecting ']' to close an attribute annotation");
            myBuilder.restoreNewlinesState();

            annotation.done(ANNOTATION);
            return true;
        }
        else if (allowShortAnnotations && at(IDENTIFIER)) {
            parseAnnotationEntry();
            return true;
        }
        return false;
    }

    /*
     * annotationEntry
     *   : SimpleName{"."} typeArguments? valueArguments?
     *   ;
     */
    private void parseAnnotationEntry() {
        assert _at(IDENTIFIER);

        PsiBuilder.Marker attribute = mark();

        PsiBuilder.Marker reference = mark();
        PsiBuilder.Marker typeReference = mark();
        parseUserType();
        typeReference.done(TYPE_REFERENCE);
        reference.done(CONSTRUCTOR_CALLEE);

        parseTypeArgumentList();

        if (at(LPAR)) {
            myExpressionParsing.parseValueArgumentList();
        }
        attribute.done(ANNOTATION_ENTRY);
    }

    /*
     * class
     *   : modifiers ("class" | "trait") SimpleName
     *       typeParameters?
     *         modifiers ("(" primaryConstructorParameter{","} ")")?
     *       (":" attributes delegationSpecifier{","})?
     *       typeConstraints
     *       (classBody? | enumClassBody)
     *   ;
     */
    IElementType parseClass(boolean enumClass) {
        assert _atSet(CLASS_KEYWORD, TRAIT_KEYWORD);
        advance(); // CLASS_KEYWORD or TRAIT_KEYWORD

        expect(IDENTIFIER, "Class name expected", CLASS_NAME_RECOVERY_SET);
        boolean typeParametersDeclared = parseTypeParameterList(TYPE_PARAMETER_GT_RECOVERY_SET);

        PsiBuilder.Marker beforeConstructorModifiers = mark();
        boolean hasConstructorModifiers = parseModifierList(PRIMARY_CONSTRUCTOR_MODIFIER_LIST, false);

        // Some modifiers found, but no parentheses following: class has already ended, and we are looking at something else
        if (hasConstructorModifiers && !atSet(LPAR, LBRACE, COLON) ) {
            beforeConstructorModifiers.rollbackTo();
            return CLASS;
        }

        // We are still inside a class declaration
        beforeConstructorModifiers.drop();

        if (at(LPAR)) {
            parseValueParameterList(false, TokenSet.create(COLON, LBRACE));
        }
        else if (hasConstructorModifiers) {
            // A comprehensive error message for cases like:
            //    class A private : Foo
            // or
            //    class A private {
            error("Expecting primary constructor parameter list");
        }

        if (at(COLON)) {
            advance(); // COLON
            parseDelegationSpecifierList();
        }

        parseTypeConstraintsGuarded(typeParametersDeclared);

        if (at(LBRACE)) {
            if (enumClass) {
                parseEnumClassBody();
            }
            else {
                parseClassBody();
            }
        }

        return CLASS;
    }

    /*
     * enumClassBody
     *   : "{" (enumEntry | memberDeclaration)* "}"
     *   ;
     */
    private void parseEnumClassBody() {
        if (!at(LBRACE)) return;

        PsiBuilder.Marker classBody = mark();

        myBuilder.enableNewlines();
        advance(); // LBRACE

        while (!eof() && !at(RBRACE)) {
            PsiBuilder.Marker entryOrMember = mark();

            TokenSet constructorNameFollow = TokenSet.create(SEMICOLON, COLON, LPAR, LT, LBRACE);
            int lastId = findLastBefore(ENUM_MEMBER_FIRST, constructorNameFollow, false);
            TokenDetector enumDetector = new TokenDetector(ENUM_KEYWORD);
            createTruncatedBuilder(lastId).parseModifierList(MODIFIER_LIST, enumDetector, false);

            IElementType type;
            if (at(IDENTIFIER)) {
                parseEnumEntry();
                type = ENUM_ENTRY;
            }
            else {
                type = parseMemberDeclarationRest(enumDetector.isDetected());
            }

            if (type == null) {
                errorAndAdvance("Expecting an enum entry or member declaration");
                entryOrMember.drop();
            }
            else {
                entryOrMember.done(type);
            }
        }

        expect(RBRACE, "Expecting '}' to close enum class body");
        myBuilder.restoreNewlinesState();

        classBody.done(CLASS_BODY);
    }

    /*
     * enumEntry
     *   : modifiers SimpleName (":" initializer{","})? classBody?
     *   ;
     */
    private void parseEnumEntry() {
        assert _at(IDENTIFIER);

        PsiBuilder.Marker nameAsDeclaration = mark();
        advance(); // IDENTIFIER
        nameAsDeclaration.done(OBJECT_DECLARATION_NAME);

        if (at(COLON)) {
            advance(); // COLON

            parseInitializerList();
        }

        if (at(LBRACE)) {
            parseClassBody();
        }

        consumeIf(SEMICOLON);
    }

    /*
     * classBody
     *   : ("{" memberDeclaration* "}")?
     *   ;
     */
    private void parseClassBody() {
        PsiBuilder.Marker body = mark();

        myBuilder.enableNewlines();

        if (expect(LBRACE, "Expecting a class body")) {
            while (!eof()) {
                if (at(RBRACE)) {
                    break;
                }
                parseMemberDeclaration();
            }
            expect(RBRACE, "Missing '}");
        }

        myBuilder.restoreNewlinesState();

        body.done(CLASS_BODY);
    }

    /*
     * memberDeclaration
     *   : modifiers memberDeclaration'
     *   ;
     *
     * memberDeclaration'
     *   : classObject
     *   : constructor
     *   : function
     *   : property
     *   : class
     *   : extension
     *   : typedef
     *   : anonymousInitializer
     *   : object
     *   ;
     */
    private void parseMemberDeclaration() {
        PsiBuilder.Marker decl = mark();

        TokenDetector enumDetector = new TokenDetector(ENUM_KEYWORD);
        parseModifierList(MODIFIER_LIST, enumDetector, true);

        IElementType declType = parseMemberDeclarationRest(enumDetector.isDetected());

        if (declType == null) {
            errorWithRecovery("Expecting member declaration", TokenSet.create(RBRACE));
            decl.drop();
        }
        else {
            decl.done(declType);
        }
    }

    private IElementType parseMemberDeclarationRest(boolean isEnum) {
        IElementType keywordToken = tt();
        IElementType declType = null;
        if (keywordToken == CLASS_KEYWORD) {
            if (lookahead(1) == OBJECT_KEYWORD) {
                declType = parseClassObject();
            }
            else {
                declType = parseClass(isEnum);
            }
        }
        else if (keywordToken == TRAIT_KEYWORD) {
            declType = parseClass(isEnum);
        }
        else if (keywordToken == FUN_KEYWORD) {
                declType = parseFunction();
        }
        else if (keywordToken == VAL_KEYWORD || keywordToken == VAR_KEYWORD) {
            declType = parseProperty();
        }
        else if (keywordToken == TYPE_KEYWORD) {
            declType = parseTypeDef();
        }
        else if (keywordToken == OBJECT_KEYWORD) {
            parseObject(true, true);
            declType = OBJECT_DECLARATION;
        }
        else if (keywordToken == LBRACE) {
            parseBlock();
            declType = ANONYMOUS_INITIALIZER;
        }
        return declType;
    }

    /*
     * object
     *   : "object" SimpleName? ":" delegationSpecifier{","}? classBody?
     *   ;
     */
    void parseObject(boolean named, boolean optionalBody) {
        assert _at(OBJECT_KEYWORD);

        advance(); // OBJECT_KEYWORD

        if (named) {
            PsiBuilder.Marker propertyDeclaration = mark();
            expect(IDENTIFIER, "Expecting object name", TokenSet.create(LBRACE));
            propertyDeclaration.done(OBJECT_DECLARATION_NAME);
        }
        else {
            if (at(IDENTIFIER)) {
                error("An object expression cannot bind a name");
            }
        }

        if (optionalBody) {
            if (at(COLON)) {
                advance(); // COLON
                parseDelegationSpecifierList();
            }
            if (at(LBRACE)) {
                parseClassBody();
            }
        }
        else {
            if (at(LBRACE)) {
                parseClassBody();
            }
            else {
                expect(COLON, "Expecting ':'", TokenSet.create(IDENTIFIER, PACKAGE_KEYWORD));
                parseDelegationSpecifierList();
                parseClassBody();
            }
        }
    }

    /*
     * initializer{","}
     */
    private void parseInitializerList() {
        PsiBuilder.Marker list = mark();
        while (true) {
            if (at(COMMA)) errorAndAdvance("Expecting a this or super constructor call");
            parseInitializer();
            if (!at(COMMA)) break;
            advance(); // COMMA
        }
        list.done(INITIALIZER_LIST);
    }

    /*
     * initializer
     *   : attributes "this" valueArguments
     *   : attributes constructorInvocation // type parameters may (must?) be omitted
     *   ;
     */
    private void parseInitializer() {
        PsiBuilder.Marker initializer = mark();
        parseAnnotations(false);

        IElementType type;
        if (at(THIS_KEYWORD)) {
            PsiBuilder.Marker mark = mark();
            advance(); // THIS_KEYWORD
            mark.done(THIS_CONSTRUCTOR_REFERENCE);
            type = THIS_CALL;
        }
        else if (atSet(TYPE_REF_FIRST)) {
            PsiBuilder.Marker reference = mark();
            parseTypeRef();
            reference.done(CONSTRUCTOR_CALLEE);
            type = DELEGATOR_SUPER_CALL;
        }
        else {
            errorWithRecovery("Expecting constructor call (this(...)) or supertype initializer",
                              TokenSet.orSet(TOPLEVEL_OBJECT_FIRST, TokenSet.create(RBRACE, LBRACE, COMMA, SEMICOLON)));
            initializer.drop();
            return;
        }
        myExpressionParsing.parseValueArgumentList();

        initializer.done(type);
    }

    /*
     * classObject
     *   : modifiers "class" object
     *   ;
     */
    private IElementType parseClassObject() {
        assert _at(CLASS_KEYWORD) && lookahead(1) == OBJECT_KEYWORD;

        advance(); // CLASS_KEYWORD

        PsiBuilder.Marker objectDeclaration = mark();
        parseObject(false, true);
        objectDeclaration.done(OBJECT_DECLARATION);

        return CLASS_OBJECT;
    }

    /*
     * typedef
     *   : modifiers "type" SimpleName (typeParameters typeConstraints)? "=" type
     *   ;
     */
    JetNodeType parseTypeDef() {
        assert _at(TYPE_KEYWORD);

        advance(); // TYPE_KEYWORD

        expect(IDENTIFIER, "Type name expected", TokenSet.orSet(TokenSet.create(LT, EQ, SEMICOLON), TOPLEVEL_OBJECT_FIRST));

        if (parseTypeParameterList(TYPE_PARAMETER_GT_RECOVERY_SET)) {
            parseTypeConstraints();
        }

        expect(EQ, "Expecting '='", TokenSet.orSet(TOPLEVEL_OBJECT_FIRST, TokenSet.create(SEMICOLON)));

        parseTypeRef();

        consumeIf(SEMICOLON);

        return TYPEDEF;
    }

    /*
     * variableDeclarationEntry
     *   : SimpleName (":" type)?
     *   ;
     *
     * property
     *   : modifiers ("val" | "var")
     *       typeParameters? (type "." | annotations)?
     *       ("(" variableDeclarationEntry{","} ")" | variableDeclarationEntry)
     *       typeConstraints
     *       ("by" | "=" expression SEMI?)?
     *       (getter? setter? | setter? getter?) SEMI?
     *   ;
     */
    private IElementType parseProperty() {
        return parseProperty(false);
    }

    public IElementType parseProperty(boolean local) {
        if (at(VAL_KEYWORD) || at(VAR_KEYWORD)) {
            advance(); // VAL_KEYWORD or VAR_KEYWORD
        }
        else {
            errorAndAdvance("Expecting 'val' or 'var'");
        }

        boolean typeParametersDeclared = at(LT) && parseTypeParameterList(TokenSet.create(IDENTIFIER, EQ, COLON, SEMICOLON));

        TokenSet propertyNameFollow = TokenSet.create(COLON, EQ, LBRACE, RBRACE, SEMICOLON, VAL_KEYWORD, VAR_KEYWORD, FUN_KEYWORD, CLASS_KEYWORD);

        myBuilder.disableJoiningComplexTokens();

        // TODO: extract constant
        int lastDot = matchTokenStreamPredicate(new LastBefore(
                new AtSet(DOT, SAFE_ACCESS),
                new AbstractTokenStreamPredicate() {
                    @Override
                    public boolean matching(boolean topLevel) {
                        if (topLevel && (at(EQ) || at(COLON))) return true;
                        if (topLevel && at(IDENTIFIER)) {
                            IElementType lookahead = lookahead(1);
                            return lookahead != LT && lookahead != DOT && lookahead != SAFE_ACCESS && lookahead != QUEST;
                        }
                        return false;
                    }
                }));

        PsiBuilder.Marker receiver = mark();
        parseReceiverType("property", propertyNameFollow, lastDot);

        boolean multiDeclaration = at(LPAR);
        boolean receiverTypeDeclared = lastDot != -1;

        errorIf(receiver, multiDeclaration && receiverTypeDeclared, "Receiver type is not allowed on a multi-declaration");

        if (multiDeclaration) {
            PsiBuilder.Marker multiDecl = mark();
            parseMultiDeclarationName(propertyNameFollow);
            errorIf(multiDecl, !local, "Multi-declarations are only allowed for local variables/values");
        }
        else {
            parseFunctionOrPropertyName(receiverTypeDeclared, "property", propertyNameFollow);
        }

        myBuilder.restoreJoiningComplexTokensState();

        if (at(COLON)) {
            PsiBuilder.Marker type = mark();
            advance(); // COLON
            parseTypeRef();
            errorIf(type, multiDeclaration, "Type annotations are not allowed on multi-declarations");
        }

        parseTypeConstraintsGuarded(typeParametersDeclared);

        if (local) {
            if (at(BY_KEYWORD)) {
                parsePropertyDelegate();
            }
            else if (at(EQ)) {
                advance(); // EQ
                myExpressionParsing.parseExpression();
                // "val a = 1; b" must not be an infix call of b on "val ...;"
            }
        }
        else {
            if (at(BY_KEYWORD)) {
                parsePropertyDelegate();
                consumeIf(SEMICOLON);
            }
            else if (at(EQ)) {
                advance(); // EQ
                myExpressionParsing.parseExpression();
                consumeIf(SEMICOLON);
            }

            if (parsePropertyGetterOrSetter()) {
                parsePropertyGetterOrSetter();
            }
            if (!atSet(EOL_OR_SEMICOLON, RBRACE)) {
                if (getLastToken() != SEMICOLON) {
                    errorUntil("Property getter or setter expected", TokenSet.create(EOL_OR_SEMICOLON));
                }
            }
            else {
                consumeIf(SEMICOLON);
            }
        }

        return multiDeclaration ? MULTI_VARIABLE_DECLARATION : PROPERTY;
    }

    /*
     * propertyDelegate
     *   : "by" expression
     *   ;
     */
    private void parsePropertyDelegate() {
        assert _at(BY_KEYWORD);
        PsiBuilder.Marker delegate = mark();
        advance(); // BY_KEYWORD
        myExpressionParsing.parseExpression();
        delegate.done(PROPERTY_DELEGATE);
    }

    /*
     * (SimpleName (":" type)){","}
     */
    public void parseMultiDeclarationName(TokenSet follow) {
        // Parsing multi-name, e.g.
        //   val (a, b) = foo()
        myBuilder.disableNewlines();
        advance(); // LPAR

        TokenSet recoverySet = TokenSet.orSet(PARAMETER_NAME_RECOVERY_SET, follow);
        if (!atSet(follow)) {
            while (true) {
                if (at(COMMA)) {
                    errorAndAdvance("Expecting a name");
                }
                else if (at(RPAR)) {
                    error("Expecting a name");
                    break;
                }
                PsiBuilder.Marker property = mark();
                expect(IDENTIFIER, "Expecting a name", recoverySet);

                if (at(COLON)) {
                    advance(); // COLON
                    parseTypeRef(follow);
                }
                property.done(MULTI_VARIABLE_DECLARATION_ENTRY);

                if (!at(COMMA)) break;
                advance(); // COMMA
            }
        }

        expect(RPAR, "Expecting ')'", follow);
        myBuilder.restoreNewlinesState();
    }

    /*
     * getterOrSetter
     *   : modifiers ("get" | "set")
     *   :
     *        (     "get" "(" ")"
     *           |
     *              "set" "(" modifiers parameter ")"
     *        ) functionBody
     *   ;
     */
    private boolean parsePropertyGetterOrSetter() {
        PsiBuilder.Marker getterOrSetter = mark();

        parseModifierList(MODIFIER_LIST, false);

        if (!at(GET_KEYWORD) && !at(SET_KEYWORD)) {
            getterOrSetter.rollbackTo();
            return false;
        }

        boolean setter = at(SET_KEYWORD);
        advance(); // GET_KEYWORD or SET_KEYWORD

        if (!at(LPAR)) {
            // Account for Jet-114 (val a : int get {...})
            TokenSet ACCESSOR_FIRST_OR_PROPERTY_END = TokenSet.orSet(MODIFIER_KEYWORDS, TokenSet.create(LBRACKET, GET_KEYWORD, SET_KEYWORD, EOL_OR_SEMICOLON, RBRACE));
            if (!atSet(ACCESSOR_FIRST_OR_PROPERTY_END)) {
                errorUntil("Accessor body expected", TokenSet.orSet(ACCESSOR_FIRST_OR_PROPERTY_END, TokenSet.create(LBRACE, LPAR, EQ)));
            }
            else {
                getterOrSetter.done(PROPERTY_ACCESSOR);
                return true;
            }
        }

        myBuilder.disableNewlines();
        expect(LPAR, "Expecting '('", TokenSet.create(RPAR, IDENTIFIER, COLON, LBRACE, EQ));
        if (setter) {
            PsiBuilder.Marker parameterList = mark();
            PsiBuilder.Marker setterParameter = mark();
            parseModifierListWithShortAnnotations(MODIFIER_LIST, TokenSet.create(IDENTIFIER), TokenSet.create(RPAR, COMMA, COLON));
            expect(IDENTIFIER, "Expecting parameter name", TokenSet.create(RPAR, COLON, LBRACE, EQ));

            if (at(COLON)) {
                advance();  // COLON
                parseTypeRef();
            }
            setterParameter.done(VALUE_PARAMETER);
            parameterList.done(VALUE_PARAMETER_LIST);
        }
        if (!at(RPAR)) errorUntil("Expecting ')'", TokenSet.create(RPAR, COLON, LBRACE, EQ, EOL_OR_SEMICOLON));
        expect(RPAR, "Expecting ')'", TokenSet.create(RPAR, COLON, LBRACE, EQ));
        myBuilder.restoreNewlinesState();

        if (at(COLON)) {
            advance();

            parseTypeRef();
        }

        parseFunctionBody();

        getterOrSetter.done(PROPERTY_ACCESSOR);

        return true;
    }

    /*
     * function
     *   : modifiers "fun" typeParameters?
     *       (type "." | attributes)?
     *       SimpleName
     *       typeParameters? functionParameters (":" type)?
     *       typeConstraints
     *       functionBody?
     *   ;
     */
    IElementType parseFunction() {
        assert _at(FUN_KEYWORD);

        advance(); // FUN_KEYWORD

        // Recovery for the case of class A { fun| }
        if (at(RBRACE)) {
            error("Function body expected");
            return FUN;
        }

        boolean typeParameterListOccurred = false;
        if (at(LT)) {
            parseTypeParameterList(TokenSet.create(LBRACKET, LBRACE, LPAR));
            typeParameterListOccurred = true;
        }

        myBuilder.disableJoiningComplexTokens();
        int lastDot = findLastBefore(RECEIVER_TYPE_TERMINATORS, TokenSet.create(LPAR), true);

        TokenSet functionNameFollow = TokenSet.create(LT, LPAR, COLON, EQ);
        parseReceiverType("function", functionNameFollow, lastDot);

        parseFunctionOrPropertyName(lastDot != -1, "function", functionNameFollow);

        myBuilder.restoreJoiningComplexTokensState();

        TokenSet valueParametersFollow = TokenSet.create(COLON, EQ, LBRACE, SEMICOLON, RPAR);

        if (at(LT)) {
            PsiBuilder.Marker error = mark();
            parseTypeParameterList(TokenSet.orSet(TokenSet.create(LPAR), valueParametersFollow));
            errorIf(error, typeParameterListOccurred, "Only one type parameter list is allowed for a function");
            typeParameterListOccurred = true;
        }

        if (at(LPAR)) {
            parseValueParameterList(false, valueParametersFollow);
        }
        else {
            error("Expecting '('");
        }

        if (at(COLON)) {
            advance(); // COLON

            parseTypeRef();
        }

        parseTypeConstraintsGuarded(typeParameterListOccurred);

        if (at(SEMICOLON)) {
            advance(); // SEMICOLON
        }
        else if (at(EQ) || at(LBRACE)) {
            parseFunctionBody();
        }

        return FUN;
    }

    /*
     *   (type "." | attributes)?
     */
    private void parseReceiverType(String title, TokenSet nameFollow, int lastDot) {
        if (lastDot == -1) { // There's no explicit receiver type specified
            parseAnnotations(false);
        }
        else {
            createTruncatedBuilder(lastDot).parseTypeRef();

            if (atSet(RECEIVER_TYPE_TERMINATORS)) {
                advance(); // expectation
            }
            else {
                errorWithRecovery("Expecting '.' before a " + title + " name", nameFollow);
            }
        }
    }

    /*
     * IDENTIFIER
     */
    private void parseFunctionOrPropertyName(boolean receiverFound, String title, TokenSet nameFollow) {
        if (!receiverFound) {
            expect(IDENTIFIER, "Expecting " + title + " name or receiver type", nameFollow);
        }
        else {
            expect(IDENTIFIER, "Expecting " + title + " name", nameFollow);
        }
    }

    /*
     * functionBody
     *   : block
     *   : "=" element
     *   ;
     */
    private void parseFunctionBody() {
        if (at(LBRACE)) {
            parseBlock();
        }
        else if (at(EQ)) {
            advance(); // EQ
            myExpressionParsing.parseExpression();
            consumeIf(SEMICOLON);
        }
        else {
            errorAndAdvance("Expecting function body");
        }
    }

    /*
     * block
     *   : "{" (expressions)* "}"
     *   ;
     */
    void parseBlock() {
        PsiBuilder.Marker block = mark();

        myBuilder.enableNewlines();
        expect(LBRACE, "Expecting '{' to open a block");

        myExpressionParsing.parseStatements();

        expect(RBRACE, "Expecting '}");
        myBuilder.restoreNewlinesState();

        block.done(BLOCK);
    }

    /*
     * delegationSpecifier{","}
     */
    /*package*/ void parseDelegationSpecifierList() {
        PsiBuilder.Marker list = mark();

        while (true) {
            if (at(COMMA)) {
                errorAndAdvance("Expecting a delegation specifier");
                continue;
            }
            parseDelegationSpecifier();
            if (!at(COMMA)) break;
            advance(); // COMMA
        }

        list.done(DELEGATION_SPECIFIER_LIST);
    }

    /*
     * attributes delegationSpecifier
     *
     * delegationSpecifier
     *   : constructorInvocation // type and constructor arguments
     *   : userType
     *   : explicitDelegation
     *   ;
     *
     * explicitDelegation
     *   : userType "by" element
     *   ;
     */
    private void parseDelegationSpecifier() {
        PsiBuilder.Marker delegator = mark();
        parseAnnotations(false);

        PsiBuilder.Marker reference = mark();
        parseTypeRef();

        if (at(BY_KEYWORD)) {
            reference.drop();
            advance(); // BY_KEYWORD
            createForByClause(myBuilder).myExpressionParsing.parseExpression();
            delegator.done(DELEGATOR_BY);
        }
        else if (at(LPAR)) {
            reference.done(CONSTRUCTOR_CALLEE);
            myExpressionParsing.parseValueArgumentList();
            delegator.done(DELEGATOR_SUPER_CALL);
        }
        else {
            reference.drop();
            delegator.done(DELEGATOR_SUPER_CLASS);
        }
    }

    /*
     * typeParameters
     *   : ("<" typeParameter{","} ">"
     *   ;
     */
    private boolean parseTypeParameterList(TokenSet recoverySet) {
        boolean result = false;
        if (at(LT)) {
            PsiBuilder.Marker list = mark();

            myBuilder.disableNewlines();
            advance(); // LT

            while (true) {
                if (at(COMMA)) errorAndAdvance("Expecting type parameter declaration");
                parseTypeParameter();

                if (!at(COMMA)) break;
                advance(); // COMMA
            }

            expect(GT, "Missing '>'", recoverySet);
            myBuilder.restoreNewlinesState();
            result = true;

            list.done(TYPE_PARAMETER_LIST);
        }
        return result;
    }

    /*
     * typeConstraints
     *   : ("where" typeConstraint{","})?
     *   ;
     */
    private void parseTypeConstraintsGuarded(boolean typeParameterListOccurred) {
        PsiBuilder.Marker error = mark();
        boolean constraints = parseTypeConstraints();
        errorIf(error, constraints && !typeParameterListOccurred, "Type constraints are not allowed when no type parameters declared");
    }

    private boolean parseTypeConstraints() {
        if (at(WHERE_KEYWORD)) {
            parseTypeConstraintList();
            return true;
        }
        return false;
    }

    /*
     * typeConstraint{","}
     */
    private void parseTypeConstraintList() {
        assert _at(WHERE_KEYWORD);

        advance(); // WHERE_KEYWORD

        PsiBuilder.Marker list = mark();

        while (true) {
            if (at(COMMA)) errorAndAdvance("Type constraint expected");
            parseTypeConstraint();
            if (!at(COMMA)) break;
            advance(); // COMMA
        }

        list.done(TYPE_CONSTRAINT_LIST);
    }

    /*
     * typeConstraint
     *   : attributes SimpleName ":" type
     *   : attributes "class" "object" SimpleName ":" type
     *   ;
     */
    private void parseTypeConstraint() {
        PsiBuilder.Marker constraint = mark();

        parseAnnotations(false);

        if (at(CLASS_KEYWORD)) {
            advance(); // CLASS_KEYWORD

            expect(OBJECT_KEYWORD, "Expecting 'object'", TYPE_REF_FIRST);

        }

        PsiBuilder.Marker reference = mark();
        if (expect(IDENTIFIER, "Expecting type parameter name", TokenSet.orSet(TokenSet.create(COLON, COMMA), TYPE_REF_FIRST))) {
            reference.done(REFERENCE_EXPRESSION);
        }
        else {
            reference.drop();
        }

        expect(COLON, "Expecting ':' before the upper bound", TYPE_REF_FIRST);

        parseTypeRef();

        constraint.done(TYPE_CONSTRAINT);
    }

    /*
     * typeParameter
     *   : modifiers SimpleName (":" userType)?
     *   ;
     */
    private void parseTypeParameter() {
        if (atSet(TYPE_PARAMETER_GT_RECOVERY_SET)) {
            error("Type parameter declaration expected");
            return;
        }

        PsiBuilder.Marker mark = mark();

        parseModifierListWithShortAnnotations(MODIFIER_LIST, TokenSet.create(IDENTIFIER), TokenSet.create(COMMA, GT, COLON));

        expect(IDENTIFIER, "Type parameter name expected", TokenSet.EMPTY);

        if (at(COLON)) {
            advance(); // COLON
            parseTypeRef();
        }

        mark.done(TYPE_PARAMETER);

    }

    /*
     * type
     *   : attributes typeDescriptor
     *
     * typeDescriptor
     *   : selfType
     *   : functionType
     *   : userType
     *   : tupleType
     *   : nullableType
     *   ;
     *
     * nullableType
     *   : typeDescriptor "?"
     */
    void parseTypeRef() {
        parseTypeRef(TokenSet.EMPTY);
    }

    void parseTypeRef(TokenSet extraRecoverySet) {
        PsiBuilder.Marker typeRefMarker = parseTypeRefContents(extraRecoverySet);
        typeRefMarker.done(TYPE_REFERENCE);
    }

    // The extraRecoverySet is needed for the foo(bar<x, 1, y>(z)) case, to tell whether we should stop
    // on expression-indicating symbols or not
    private PsiBuilder.Marker parseTypeRefContents(TokenSet extraRecoverySet) {
        // Disabling token merge is required for cases like
        //    Int?.(Foo) -> Bar
        // we don't support this case now
//        myBuilder.disableJoiningComplexTokens();
        PsiBuilder.Marker typeRefMarker = mark();
        parseAnnotations(false);

        if (at(IDENTIFIER) || at(PACKAGE_KEYWORD)) {
            parseUserType();
        }
        else if (at(HASH)) {
            parseTupleType();
        }
        else if (at(LPAR)) {
            PsiBuilder.Marker functionOrParenthesizedType = mark();

            // This may be a function parameter list or just a prenthesized type
            advance(); // LPAR
            parseTypeRefContents(TokenSet.EMPTY).drop(); // parenthesized types, no reference element around it is needed

            if (at(RPAR)) {
                advance(); // RPAR
                if (at(ARROW)) {
                    // It's a function type with one parameter specified
                    //    (A) -> B
                    functionOrParenthesizedType.rollbackTo();
                    parseFunctionType();
                }
                else {
                    // It's a parenthesized type
                    //    (A)
                    functionOrParenthesizedType.drop();
                }
            }
            else {
                // This must be a function type
                //   (A, B) -> C
                // or
                //   (a : A) -> C
                functionOrParenthesizedType.rollbackTo();
                parseFunctionType();
            }

        }
        else if (at(CAPITALIZED_THIS_KEYWORD)) {
            parseSelfType();
        }
        else {
            errorWithRecovery("Type expected",
                    TokenSet.orSet(TOPLEVEL_OBJECT_FIRST,
                                   TokenSet.create(EQ, COMMA, GT, RBRACKET, DOT, RPAR, RBRACE, LBRACE, SEMICOLON), extraRecoverySet));
        }

        typeRefMarker = parseNullableTypeSuffix(typeRefMarker);

        if (at(DOT)) {
            // This is a receiver for a function type
            //  A.(B) -> C
            //   ^

            PsiBuilder.Marker functionType = typeRefMarker.precede();
            PsiBuilder.Marker receiverType = typeRefMarker.precede();
            typeRefMarker.done(TYPE_REFERENCE);
            receiverType.done(FUNCTION_TYPE_RECEIVER);

            advance(); // DOT

            if (at(LPAR)) {
                parseFunctionTypeContents().drop();
            }
            else {
                error("Expecting function type");
            }
            typeRefMarker = functionType.precede();

            functionType.done(FUNCTION_TYPE);
        }
//        myBuilder.restoreJoiningComplexTokensState();
        return typeRefMarker;
    }

    @NotNull
    PsiBuilder.Marker parseNullableTypeSuffix(@NotNull PsiBuilder.Marker typeRefMarker) {
        while (at(QUEST)) {
            PsiBuilder.Marker precede = typeRefMarker.precede();
            advance(); // QUEST
            typeRefMarker.done(NULLABLE_TYPE);
            typeRefMarker = precede;
        }
        return typeRefMarker;
    }

    /*
     * userType
     *   : ("package" ".")? simpleUserType{"."}
     *   ;
     */
    void parseUserType() {
        PsiBuilder.Marker userType = mark();

        if (at(PACKAGE_KEYWORD)) {
            advance(); // PACKAGE_KEYWORD
            expect(DOT, "Expecting '.'", TokenSet.create(IDENTIFIER));
        }

        PsiBuilder.Marker reference = mark();
        while (true) {
            if (expect(IDENTIFIER, "Expecting type name", TokenSet.orSet(JetExpressionParsing.EXPRESSION_FIRST, JetExpressionParsing.EXPRESSION_FOLLOW))) {
                reference.done(REFERENCE_EXPRESSION);
            }
            else {
                reference.drop();
                break;
            }

            parseTypeArgumentList();
            if (!at(DOT)) {
                break;
            }
            if (lookahead(1) == LPAR) {
                // This may be a receiver for a function type
                //   Int.(Int) -> Int
                break;
            }

            PsiBuilder.Marker precede = userType.precede();
            userType.done(USER_TYPE);
            userType = precede;

            advance(); // DOT
            reference = mark();
        }

        userType.done(USER_TYPE);
    }

    /*
     * selfType
     *   : "This"
     *   ;
     */
    private void parseSelfType() {
        assert _at(CAPITALIZED_THIS_KEYWORD);

        PsiBuilder.Marker type = mark();
        advance(); // CAPITALIZED_THIS_KEYWORD
        type.done(SELF_TYPE);
    }

    /*
     *  (optionalProjection type){","}
     */
    private PsiBuilder.Marker parseTypeArgumentList() {
        if (!at(LT)) return null;

        PsiBuilder.Marker list = mark();

        tryParseTypeArgumentList(TokenSet.EMPTY);

        list.done(TYPE_ARGUMENT_LIST);
        return list;
    }

    boolean tryParseTypeArgumentList(TokenSet extraRecoverySet) {
        myBuilder.disableNewlines();
        advance(); // LT

        while (true) {
            PsiBuilder.Marker projection = mark();

//            TokenSet lookFor = TokenSet.create(IDENTIFIER);
//            TokenSet stopAt = TokenSet.create(COMMA, COLON, GT);
//            parseModifierListWithShortAnnotations(MODIFIER_LIST, lookFor, stopAt);
            // Currently we do not allow annotations
            parseModifierList(MODIFIER_LIST, false);

            if (at(MUL)) {
                advance(); // MUL
            }
            else {
                parseTypeRef(extraRecoverySet);
            }
            projection.done(TYPE_PROJECTION);
            if (!at(COMMA)) break;
            advance(); // COMMA
        }

        boolean atGT = at(GT);
        if (!atGT) {
            error("Expecting a '>'");
        }
        else {
            advance(); // GT
        }
        myBuilder.restoreNewlinesState();
        return atGT;
    }

    private void parseModifierListWithShortAnnotations(IElementType modifierList, TokenSet lookFor, TokenSet stopAt) {
        int lastId = findLastBefore(lookFor, stopAt, false);
        createTruncatedBuilder(lastId).parseModifierList(modifierList, true);
    }

    /*
     * tupleType
     *   : "#" "(" type{","}? ")"
     *   : "#" "(" parameter{","} ")" // tuple with named entries, the names do not affect assignment compatibility
     *   ;
     */
    @Deprecated // Tuples are dropped, but parsing is left to minimize surprising. This code should be removed some time (in Kotlin 1.0?)
    private void parseTupleType() {
        assert _at(HASH);

        PsiBuilder.Marker tuple = mark();

        myBuilder.disableNewlines();
        advance(); // HASH
        consumeIf(LPAR);

        if (!at(RPAR)) {
            while (true) {
                if (at(COLON)) {
                    errorAndAdvance("Expecting a name for tuple entry");
                }

                if (at(IDENTIFIER) && lookahead(1) == COLON) {
                    advance(); // IDENTIFIER
                    advance(); // COLON
                    parseTypeRef();
                }
                else if (TYPE_REF_FIRST.contains(tt())) {
                    parseTypeRef();
                }
                else {
                    error("Type expected");
                    break;
                }
                if (!at(COMMA)) break;
                advance(); // COMMA
            }
        }

        consumeIf(RPAR);
        myBuilder.restoreNewlinesState();

        tuple.error("Tuples are not supported. Use data classes instead.");
    }

    /*
     * functionType
     *   : "(" (parameter | modifiers type){","}? ")" "->" type?
     *   ;
     */
    private void parseFunctionType() {
        parseFunctionTypeContents().done(FUNCTION_TYPE);
    }

    private PsiBuilder.Marker parseFunctionTypeContents() {
        assert _at(LPAR) : tt();
        PsiBuilder.Marker functionType = mark();

//        advance(); // LPAR
//
//        int lastLPar = findLastBefore(TokenSet.create(LPAR), TokenSet.create(COLON), false);
//        if (lastLPar >= 0 && lastLPar > myBuilder.getCurrentOffset()) {
//            TODO : -1 is a hack?
//            createTruncatedBuilder(lastLPar - 1).parseTypeRef();
//            advance(); // DOT
//        }

        parseValueParameterList(true, TokenSet.EMPTY);

//        if (at(COLON)) {
//            advance(); // COLON // expect(COLON, "Expecting ':' followed by a return type", TYPE_REF_FIRST);

        expect(ARROW, "Expecting '->' to specify return type of a function type", TYPE_REF_FIRST);
        parseTypeRef();
//        }

        return functionType;//.done(FUNCTION_TYPE);
    }

    /*
     * functionParameters
     *   : "(" functionParameter{","}? ")" // default values
     *   ;
     *
     * functionParameter
     *   : modifiers functionParameterRest
     *   ;
     *
     * functionParameterRest
     *   : parameter ("=" element)?
     *   ;
     */
    void parseValueParameterList(boolean isFunctionTypeContents, TokenSet recoverySet) {
        assert _at(LPAR);
        PsiBuilder.Marker parameters = mark();

        myBuilder.disableNewlines();
        advance(); // LPAR

        if (!at(RPAR) && !atSet(recoverySet)) {
            while (true) {
                if (at(COMMA)) {
                    errorAndAdvance("Expecting a parameter declaration");
                }
                else if (at(RPAR)) {
                    error("Expecting a parameter declaration");
                    break;
                }

                if (isFunctionTypeContents) {
                    if (!tryParseValueParameter()) {
                        PsiBuilder.Marker valueParameter = mark();
                        parseModifierList(MODIFIER_LIST, false); // lazy, out, ref
                        parseTypeRef();
                        valueParameter.done(VALUE_PARAMETER);
                    }
                }
                else {
                    parseValueParameter();
                }

                if (at(COMMA)) {
                    advance(); // COMMA
                }
                else {
                    if (!at(RPAR)) error("Expecting comma or ')'");
                    if (!atSet(isFunctionTypeContents ? LAMBDA_VALUE_PARAMETER_FIRST : VALUE_PARAMETER_FIRST)) break;
                }
            }
        }

        expect(RPAR, "Expecting ')'", recoverySet);
        myBuilder.restoreNewlinesState();

        parameters.done(VALUE_PARAMETER_LIST);
    }

    /*
     * functionParameter
     *   : modifiers ("val" | "var")? parameter ("=" element)?
     *   ;
     */
    private boolean tryParseValueParameter() {
        return parseValueParameter(true);
    }

    public void parseValueParameter() {
        parseValueParameter(false);
    }

    private boolean parseValueParameter(boolean rollbackOnFailure) {
        PsiBuilder.Marker parameter = mark();

        parseModifierListWithShortAnnotations(MODIFIER_LIST, TokenSet.create(IDENTIFIER), TokenSet.create(COMMA, RPAR, COLON));

        if (at(VAR_KEYWORD) || at(VAL_KEYWORD)) {
            advance(); // VAR_KEYWORD | VAL_KEYWORD
        }

        if (!parseFunctionParameterRest() && rollbackOnFailure) {
            parameter.rollbackTo();
            return false;
        }

        parameter.done(VALUE_PARAMETER);
        return true;
    }

    /*
     * functionParameterRest
     *   : parameter ("=" element)?
     *   ;
     */
    private boolean parseFunctionParameterRest() {
        boolean noErrors = true;

        // Recovery for the case 'fun foo(Array<String>) {}'
        if (at(IDENTIFIER) && lookahead(1) == LT) {
            error("Parameter name expected");
            parseTypeRef();
            noErrors = false;
        }
        else {
            expect(IDENTIFIER, "Parameter name expected", PARAMETER_NAME_RECOVERY_SET);

            if (at(COLON)) {
                advance(); // COLON
                parseTypeRef();
            }
            else {
                errorWithRecovery("Parameters must have type annotation", PARAMETER_NAME_RECOVERY_SET);
                noErrors = false;
            }
        }

        if (at(EQ)) {
            advance(); // EQ
            myExpressionParsing.parseExpression();
        }

        return noErrors;
    }

    @Override
    protected JetParsing create(SemanticWhitespaceAwarePsiBuilder builder) {
        return createForTopLevel(builder);
    }

    /*package*/ static class TokenDetector implements Consumer<IElementType> {

        private boolean detected = false;
        private final TokenSet tokens;

        public TokenDetector(JetKeywordToken token) {
            this.tokens = TokenSet.create(token);
        }

        @Override
        public void consume(IElementType item) {
            if (tokens.contains(item)) {
                detected = true;
            }
        }

        public boolean isDetected() {
            return detected;
        }
    }
}
