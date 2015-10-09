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

package org.jetbrains.kotlin.parsing;

import com.intellij.lang.PsiBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.JetNodeType;
import org.jetbrains.kotlin.lexer.JetKeywordToken;
import org.jetbrains.kotlin.lexer.JetTokens;

import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.kotlin.JetNodeTypes.*;
import static org.jetbrains.kotlin.lexer.JetTokens.*;
import static org.jetbrains.kotlin.parsing.JetParsing.AnnotationParsingMode.*;
import static org.jetbrains.kotlin.parsing.JetParsing.DeclarationParsingMode.*;

public class JetParsing extends AbstractJetParsing {
    private static final Logger LOG = Logger.getInstance(JetParsing.class);

    // TODO: token sets to constants, including derived methods
    public static final Map<String, IElementType> MODIFIER_KEYWORD_MAP = new HashMap<String, IElementType>();
    static {
        for (IElementType softKeyword : MODIFIER_KEYWORDS.getTypes()) {
            MODIFIER_KEYWORD_MAP.put(((JetKeywordToken) softKeyword).getValue(), softKeyword);
        }
    }

    private static final TokenSet TOP_LEVEL_DECLARATION_FIRST = TokenSet.create(
            TYPE_ALIAS_KEYWORD, INTERFACE_KEYWORD, CLASS_KEYWORD, OBJECT_KEYWORD,
            FUN_KEYWORD, VAL_KEYWORD, PACKAGE_KEYWORD);
    private static final TokenSet DECLARATION_FIRST = TokenSet.orSet(TOP_LEVEL_DECLARATION_FIRST,
                                                                     TokenSet.create(INIT_KEYWORD, GET_KEYWORD, SET_KEYWORD, CONSTRUCTOR_KEYWORD));

    private static final TokenSet CLASS_NAME_RECOVERY_SET = TokenSet.orSet(TokenSet.create(LT, LPAR, COLON, LBRACE),
                                                                           TOP_LEVEL_DECLARATION_FIRST);
    private static final TokenSet TYPE_PARAMETER_GT_RECOVERY_SET = TokenSet.create(WHERE_KEYWORD, LPAR, COLON, LBRACE, GT);
    private static final TokenSet PARAMETER_NAME_RECOVERY_SET = TokenSet.create(COLON, EQ, COMMA, RPAR);
    private static final TokenSet PACKAGE_NAME_RECOVERY_SET = TokenSet.create(DOT, EOL_OR_SEMICOLON);
    private static final TokenSet IMPORT_RECOVERY_SET = TokenSet.create(AS_KEYWORD, DOT, EOL_OR_SEMICOLON);
    /*package*/ static final TokenSet TYPE_REF_FIRST = TokenSet.create(LBRACKET, IDENTIFIER, LPAR, CAPITALIZED_THIS_KEYWORD, HASH, DYNAMIC_KEYWORD);
    private static final TokenSet RECEIVER_TYPE_TERMINATORS = TokenSet.create(DOT, SAFE_ACCESS);
    private static final TokenSet VALUE_PARAMETER_FIRST =
            TokenSet.orSet(TokenSet.create(IDENTIFIER, LBRACKET, VAL_KEYWORD, VAR_KEYWORD), MODIFIER_KEYWORDS);
    private static final TokenSet LAMBDA_VALUE_PARAMETER_FIRST =
            TokenSet.orSet(TokenSet.create(IDENTIFIER, LBRACKET), MODIFIER_KEYWORDS);
    private static final TokenSet SOFT_KEYWORDS_AT_MEMBER_START = TokenSet.create(CONSTRUCTOR_KEYWORD, INIT_KEYWORD);
    private static final TokenSet ANNOTATION_TARGETS = TokenSet.create(
            FILE_KEYWORD, FIELD_KEYWORD, GET_KEYWORD, SET_KEYWORD, PROPERTY_KEYWORD, RECEIVER_KEYWORD, PARAM_KEYWORD, SETPARAM_KEYWORD);

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
            parseTopLevelDeclaration();
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
     *  : fileAnnotationList? packageDirective? import*
     *  ;
     */
    private void parsePreamble() {
        PsiBuilder.Marker firstEntry = mark();

        /*
         * fileAnnotationList
         *   : fileAnnotations*
         */
        parseFileAnnotationList(FILE_ANNOTATIONS_BEFORE_PACKAGE);

        /*
         * packageDirective
         *   : modifiers "package" SimpleName{"."} SEMI?
         *   ;
         */
        PsiBuilder.Marker packageDirective = mark();
        parseModifierList(ALLOW_UNESCAPED_REGULAR_ANNOTATIONS);

        if (at(PACKAGE_KEYWORD)) {
            advance(); // PACKAGE_KEYWORD

            parsePackageName();

            firstEntry.drop();

            consumeIf(SEMICOLON);

            packageDirective.done(PACKAGE_DIRECTIVE);
        }
        else {
            // When package directive is omitted we should not report error on non-file annotations at the beginning of the file.
            // So, we rollback the parsing position and reparse file annotation list without report error on non-file annotations.
            firstEntry.rollbackTo();

            parseFileAnnotationList(FILE_ANNOTATIONS_WHEN_PACKAGE_OMITTED);
            packageDirective = mark();
            packageDirective.done(PACKAGE_DIRECTIVE);
            // this is necessary to allow comments at the start of the file to be bound to the first declaration
            packageDirective.setCustomEdgeTokenBinders(DoNotBindAnything.INSTANCE$, null);
        }

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

            if (at(DOT)) {
                advance(); // DOT
                qualifiedExpression.error("Package name must be a '.'-separated identifier list");
                qualifiedExpression = mark();
                continue;
            }

            PsiBuilder.Marker nsName = mark();
            boolean simpleNameFound = expect(IDENTIFIER, "Package name must be a '.'-separated identifier list", PACKAGE_NAME_RECOVERY_SET);
            if (simpleNameFound) {
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
                advance(); // DOT

                if (simpleName && !simpleNameFound) {
                    qualifiedExpression.drop();
                    qualifiedExpression = mark();
                }
                else {
                    simpleName = false;
                }
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

        if (!at(IDENTIFIER)) {
            PsiBuilder.Marker error = mark();
            skipUntil(TokenSet.create(EOL_OR_SEMICOLON));
            error.error("Expecting qualified name");
            importDirective.done(IMPORT_DIRECTIVE);
            consumeIf(SEMICOLON);
            return;
        }

        PsiBuilder.Marker qualifiedName = mark();
        PsiBuilder.Marker reference = mark();
        advance(); // IDENTIFIER
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
        importDirective.setCustomEdgeTokenBinders(null, TrailingCommentsBinder.INSTANCE$);
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
        PsiBuilder.Marker importList = mark();
        if (!at(IMPORT_KEYWORD)) {
            // this is necessary to allow comments at the start of the file to be bound to the first declaration
            importList.setCustomEdgeTokenBinders(DoNotBindAnything.INSTANCE$, null);
        }
        while (at(IMPORT_KEYWORD)) {
            parseImportDirective();
        }
        importList.done(IMPORT_LIST);
    }

    /*
     * toplevelObject
     *   : package
     *   : class
     *   : extension
     *   : function
     *   : property
     *   : typeAlias
     *   : object
     *   ;
     */
    private void parseTopLevelDeclaration() {
        if (at(SEMICOLON)) {
            advance(); // SEMICOLON
            return;
        }
        PsiBuilder.Marker decl = mark();

        ModifierDetector detector = new ModifierDetector();
        parseModifierList(detector, ALLOW_UNESCAPED_REGULAR_ANNOTATIONS);

        IElementType keywordToken = tt();
        IElementType declType = null;
//        if (keywordToken == PACKAGE_KEYWORD) {
//            declType = parsePackageBlock();
//        }
//        else
        if (keywordToken == CLASS_KEYWORD || keywordToken == INTERFACE_KEYWORD) {
            declType = parseClass(detector.isEnumDetected(), TOP_LEVEL);
        }
        else if (keywordToken == FUN_KEYWORD) {
            declType = parseFunction();
        }
        else if (keywordToken == VAL_KEYWORD || keywordToken == VAR_KEYWORD) {
            declType = parseProperty();
        }
        else if (keywordToken == TYPE_ALIAS_KEYWORD) {
            declType = parseTypeAlias();
        }
        else if (keywordToken == OBJECT_KEYWORD) {
            parseObject(NameParsingMode.REQUIRED, true, TOP_LEVEL);
            declType = OBJECT_DECLARATION;
        }
        else if (at(LBRACE)) {
            error("Expecting a top level declaration");
            parseBlock();
            declType = FUN;
        }

        if (declType == null) {
            errorAndAdvance("Expecting a top level declaration");
            decl.drop();
        }
        else {
            closeDeclarationWithCommentBinders(decl, declType, true);
        }
    }

    /*
     * (modifier | annotation)*
     */
    boolean parseModifierList(
            @NotNull AnnotationParsingMode annotationParsingMode
    ) {
        return parseModifierList(null, annotationParsingMode);
    }

    /**
     * (modifier | annotation)*
     *
     * Feeds modifiers (not annotations) into the passed consumer, if it is not null
     */
    boolean parseModifierList(
            @Nullable Consumer<IElementType> tokenConsumer,
            @NotNull AnnotationParsingMode annotationParsingMode
    ) {
        PsiBuilder.Marker list = mark();
        boolean empty = true;
        while (!eof()) {
            if (annotationParsingMode.atMemberStart && atSet(SOFT_KEYWORDS_AT_MEMBER_START)) break;
            if ((annotationParsingMode == PRIMARY_CONSTRUCTOR_MODIFIER_LIST || annotationParsingMode == PRIMARY_CONSTRUCTOR_MODIFIER_LIST_LOCAL) &&
                atSet(CONSTRUCTOR_KEYWORD, WHERE_KEYWORD)) break;

            if (at(AT) && annotationParsingMode.allowAnnotations) {
                parseAnnotationOrList(annotationParsingMode);
            }
            else if (tryParseModifier(tokenConsumer)) {
                // modifier advanced
            }
            else if (annotationParsingMode.allowShortAnnotations && at(IDENTIFIER)) {
                error("Use '@' symbol before annotations");
                parseAnnotation(annotationParsingMode);
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
            list.done(MODIFIER_LIST);
        }
        return !empty;
    }

    private boolean tryParseModifier(@Nullable Consumer<IElementType> tokenConsumer) {
        PsiBuilder.Marker marker = mark();

        if (atSet(MODIFIER_KEYWORDS)) {
            IElementType tt = tt();
            if (tokenConsumer != null) {
                tokenConsumer.consume(tt);
            }
            advance(); // MODIFIER
            marker.collapse(tt);
            return true;
        }

        marker.rollbackTo();
        return false;
    }

    /*
     * fileAnnotationList
     *   : ("[" "file:" annotationEntry+ "]")*
     *   ;
     */
    private void parseFileAnnotationList(AnnotationParsingMode mode) {
        if (!mode.isFileAnnotationParsingMode) {
            LOG.error("expected file annotation parsing mode, but:" + mode);
        }

        PsiBuilder.Marker fileAnnotationsList = mark();

        if (parseAnnotations(mode)) {
            fileAnnotationsList.done(FILE_ANNOTATION_LIST);
        }
        else {
            fileAnnotationsList.drop();
        }
    }

    /*
     * annotations
     *   : (annotation | annotationList)*
     *   ;
     */
    boolean parseAnnotations(AnnotationParsingMode mode) {
        if (!parseAnnotationOrList(mode)) return false;

        while (parseAnnotationOrList(mode)) {
            // do nothing
        }

        return true;
    }

    /*
     * annotation
     *   : "@" (annotationUseSiteTarget ":")? unescapedAnnotation
     *   ;
     *
     * annotationList
     *   : "@" (annotationUseSiteTarget ":")? "[" unescapedAnnotation+ "]"
     *   ;
     *
     *   annotationUseSiteTarget
     *   : "file"
     *   : "field"
     *   : "property"
     *   : "get"
     *   : "set"
     *   : "param"
     *   : "sparam"
     *   ;
     */
    private boolean parseAnnotationOrList(AnnotationParsingMode mode) {
        if (at(AT)) {
            IElementType nextRawToken = myBuilder.rawLookup(1);
            IElementType tokenToMatch = nextRawToken;
            boolean isTargetedAnnotation = false;

            if ((nextRawToken == IDENTIFIER || ANNOTATION_TARGETS.contains(nextRawToken)) && lookahead(2) == COLON) {
                tokenToMatch = lookahead(3);
                isTargetedAnnotation = true;
            }
            else if (lookahead(1) == COLON) {
                // recovery for "@:ann"
                isTargetedAnnotation = true;
                tokenToMatch = lookahead(2);
            }

            if (tokenToMatch == IDENTIFIER) {
                return parseAnnotation(mode);
            }
            else if (tokenToMatch == LBRACKET) {
                return parseAnnotationList(mode);
            }
            else {
                if (isTargetedAnnotation) {
                    if (lookahead(1) == COLON) {
                        errorAndAdvance("Expected annotation identifier after ':'", 2); // AT, COLON
                    }
                    else {
                        errorAndAdvance("Expected annotation identifier after ':'", 3); // AT, (ANNOTATION TARGET KEYWORD), COLON
                    }
                }
                else {
                    errorAndAdvance("Expected annotation identifier after '@'", 1); // AT
                }
            }
            return true;
        }

        return false;
    }

    private boolean parseAnnotationList(AnnotationParsingMode mode) {
        assert _at(AT);
        PsiBuilder.Marker annotation = mark();

        myBuilder.disableNewlines();

        advance(); // AT

        if (!parseAnnotationTargetIfNeeded(mode)) {
            annotation.rollbackTo();
            myBuilder.restoreNewlinesState();
            return false;
        }

        assert _at(LBRACKET);
        advance(); // LBRACKET

        if (!at(IDENTIFIER) && !at(AT)) {
            error("Expecting a list of annotations");
        }
        else {
            while (at(IDENTIFIER) || at(AT)) {
                if (at(AT)) {
                    errorAndAdvance("No '@' needed in annotation list"); // AT
                    continue;
                }

                parseAnnotation(ALLOW_UNESCAPED_REGULAR_ANNOTATIONS);
                while (at(COMMA)) {
                    errorAndAdvance("No commas needed to separate annotations");
                }
            }
        }

        expect(RBRACKET, "Expecting ']' to close the annotation list");
        myBuilder.restoreNewlinesState();

        annotation.done(ANNOTATION);
        return true;
    }

    // Returns true if we should continue parse annotation
    private boolean parseAnnotationTargetIfNeeded(AnnotationParsingMode mode) {
        String expectedAnnotationTargetBeforeColon = "Expected annotation target before ':'";

        if (at(COLON)) {
            // recovery for "@:ann"
            errorAndAdvance(expectedAnnotationTargetBeforeColon); // COLON
            return true;
        }

        JetKeywordToken targetKeyword = atTargetKeyword();
        if (mode == FILE_ANNOTATIONS_WHEN_PACKAGE_OMITTED && !(targetKeyword == FILE_KEYWORD && lookahead(1) == COLON)) {
            return false;
        }

        if (lookahead(1) == COLON && targetKeyword == null && at(IDENTIFIER)) {
            // recovery for "@fil:ann"
            errorAndAdvance(expectedAnnotationTargetBeforeColon); // IDENTIFIER
            advance(); // COLON
            return true;
        }

        if (targetKeyword == null && mode.isFileAnnotationParsingMode) {
            parseAnnotationTarget(mode, FILE_KEYWORD);
        }
        else if (targetKeyword != null) {
            parseAnnotationTarget(mode, targetKeyword);
        }

        return true;
    }

    private void parseAnnotationTarget(AnnotationParsingMode mode, JetKeywordToken keyword) {
        if (keyword == FILE_KEYWORD && !mode.isFileAnnotationParsingMode && at(keyword) && lookahead(1) == COLON) {
            errorAndAdvance(AT.getValue() + keyword.getValue() + " annotations are only allowed before package declaration", 2);
            return;
        }

        String message = "Expecting \"" + keyword.getValue() + COLON.getValue() + "\" prefix for " + keyword.getValue() + " annotations";

        PsiBuilder.Marker marker = mark();

        if (!expect(keyword, message)) {
            marker.drop();
        }
        else {
            marker.done(ANNOTATION_TARGET);
        }

        expect(COLON, message, TokenSet.create(IDENTIFIER, RBRACKET, LBRACKET));
    }

    @Nullable
    private JetKeywordToken atTargetKeyword() {
        for (IElementType target : ANNOTATION_TARGETS.getTypes()) {
            if (at(target)) return (JetKeywordToken) target;
        }
        return null;
    }

    /*
     * annotation
     *   : "@" (annotationUseSiteTarget ":")? unescapedAnnotation
     *   ;
     *
     * unescapedAnnotation
     *   : SimpleName{"."} typeArguments? valueArguments?
     *   ;
     */
    private boolean parseAnnotation(AnnotationParsingMode mode) {
        assert _at(IDENTIFIER) ||
               (_at(AT) && !WHITE_SPACE_OR_COMMENT_BIT_SET.contains(myBuilder.rawLookup(1)));

        PsiBuilder.Marker annotation = mark();

        boolean atAt = at(AT);
        if (atAt) {
            advance(); // AT
        }

        if (atAt && !parseAnnotationTargetIfNeeded(mode)) {
            annotation.rollbackTo();
            return false;
        }

        PsiBuilder.Marker reference = mark();
        PsiBuilder.Marker typeReference = mark();
        parseUserType();
        typeReference.done(TYPE_REFERENCE);
        reference.done(CONSTRUCTOR_CALLEE);

        parseTypeArgumentList();

        if (at(LPAR)) {
            myExpressionParsing.parseValueArgumentList();
        }
        annotation.done(ANNOTATION_ENTRY);

        return true;
    }

    public enum NameParsingMode {
        REQUIRED,
        ALLOWED,
        PROHIBITED
    }

    public enum DeclarationParsingMode {
        TOP_LEVEL,
        CLASS_MEMBER,
        LOCAL
    }

    /*
     * class
     *   : modifiers ("class" | "interface") SimpleName
     *       typeParameters?
     *       primaryConstructor?
     *       (":" annotations delegationSpecifier{","})?
     *       typeConstraints
     *       (classBody? | enumClassBody)
     *   ;
     *
     * primaryConstructor
     *   : (modifiers "constructor")? ("(" functionParameter{","} ")")
     *   ;
     *
     * object
     *   : "object" SimpleName? primaryConstructor? ":" delegationSpecifier{","}? classBody?
     *   ;
     */
    IElementType parseClassOrObject(
            boolean object,
            NameParsingMode nameParsingMode,
            boolean optionalBody,
            boolean enumClass,
            DeclarationParsingMode declarationParsingMode
    ) {
        if (object) {
            assert _at(OBJECT_KEYWORD);
        }
        else {
            assert _atSet(CLASS_KEYWORD, INTERFACE_KEYWORD);
        }
        advance(); // CLASS_KEYWORD, INTERFACE_KEYWORD or OBJECT_KEYWORD

        if (nameParsingMode == NameParsingMode.REQUIRED) {
            OptionalMarker marker = new OptionalMarker(object);
            expect(IDENTIFIER, "Name expected", CLASS_NAME_RECOVERY_SET);
            marker.done(OBJECT_DECLARATION_NAME);
        }
        else {
            assert object : "Must be an object to be nameless";
            if (at(IDENTIFIER)) {
                if (nameParsingMode == NameParsingMode.PROHIBITED) {
                    errorAndAdvance("An object expression cannot bind a name");
                }
                else {
                    assert nameParsingMode == NameParsingMode.ALLOWED;
                    PsiBuilder.Marker marker = mark();
                    advance();
                    marker.done(OBJECT_DECLARATION_NAME);
                }
            }
        }

        OptionalMarker typeParamsMarker = new OptionalMarker(object);
        boolean typeParametersDeclared = parseTypeParameterList(TYPE_PARAMETER_GT_RECOVERY_SET);
        typeParamsMarker.error("Type parameters are not allowed for objects");

        PsiBuilder.Marker beforeConstructorModifiers = mark();
        PsiBuilder.Marker primaryConstructorMarker = mark();
        boolean hasConstructorModifiers = parseModifierList(
                declarationParsingMode != LOCAL ? PRIMARY_CONSTRUCTOR_MODIFIER_LIST : PRIMARY_CONSTRUCTOR_MODIFIER_LIST_LOCAL
        );

        // Some modifiers found, but no parentheses following: class has already ended, and we are looking at something else
        if (hasConstructorModifiers && !atSet(LPAR, LBRACE, COLON, CONSTRUCTOR_KEYWORD)) {
            beforeConstructorModifiers.rollbackTo();
            return object ? OBJECT_DECLARATION : CLASS;
        }

        // We are still inside a class declaration
        beforeConstructorModifiers.drop();

        boolean hasConstructorKeyword = at(CONSTRUCTOR_KEYWORD);
        if (hasConstructorKeyword) {
            advance(); // CONSTRUCTOR_KEYWORD
        }

        if (at(LPAR)) {
            parseValueParameterList(false, /* typeRequired  = */ true, TokenSet.create(LBRACE, RBRACE));
            primaryConstructorMarker.done(PRIMARY_CONSTRUCTOR);
        }
        else if (hasConstructorModifiers || hasConstructorKeyword) {
            // A comprehensive error message for cases like:
            //    class A private : Foo
            // or
            //    class A private {
            primaryConstructorMarker.done(PRIMARY_CONSTRUCTOR);
            if (hasConstructorKeyword) {
                error("Expecting primary constructor parameter list");
            }
            else {
                error("Expecting 'constructor' keyword");
            }
        }
        else {
            primaryConstructorMarker.drop();
        }

        if (at(COLON)) {
            advance(); // COLON
            parseDelegationSpecifierList();
        }

        OptionalMarker whereMarker = new OptionalMarker(object);
        parseTypeConstraintsGuarded(typeParametersDeclared);
        whereMarker.error("Where clause is not allowed for objects");

        if (at(LBRACE)) {
            if (enumClass) {
                parseEnumClassBody();
            }
            else {
                parseClassBody();
            }
        }
        else if (!optionalBody) {
            PsiBuilder.Marker fakeBody = mark();
            error("Expecting a class body");
            fakeBody.done(CLASS_BODY);
        }

        return object ? OBJECT_DECLARATION : CLASS;
    }

    IElementType parseClass(boolean enumClass, DeclarationParsingMode declarationParsingMode) {
        return parseClassOrObject(false, NameParsingMode.REQUIRED, true, enumClass, declarationParsingMode);
    }

    void parseObject(NameParsingMode nameParsingMode, boolean optionalBody, DeclarationParsingMode declarationParsingMode) {
        parseClassOrObject(true, nameParsingMode, optionalBody, false, declarationParsingMode);
    }

    /*
     * enumClassBody
     *   : "{" enumEntries (";" members)? "}"
     *   ;
     */
    private void parseEnumClassBody() {
        if (!at(LBRACE)) return;

        PsiBuilder.Marker body = mark();
        myBuilder.enableNewlines();

        advance(); // LBRACE

        if (!parseEnumEntries() && !at(RBRACE)) {
            error("Expecting ';' after the last enum entry or '}' to close enum class body");
        }
        parseMembers();
        expect(RBRACE, "Expecting '}' to close enum class body");

        myBuilder.restoreNewlinesState();
        body.done(CLASS_BODY);
    }

    /**
     * enumEntries
     *   : enumEntry{","}?
     *   ;
     *
     * @return true if enum regular members can follow, false otherwise
     */
    private boolean parseEnumEntries() {
        while (!eof() && !at(RBRACE)) {
            switch (parseEnumEntry()) {
                case FAILED:
                    // Special case without any enum entries but with possible members after semicolon
                    if (at(SEMICOLON)) {
                        advance();
                        return true;
                    }
                    else {
                        return false;
                    }
                case NO_DELIMITER:
                    return false;
                case COMMA_DELIMITER:
                    break;
                case SEMICOLON_DELIMITER:
                    return true;
            }
        }
        return false;
    }

    private enum ParseEnumEntryResult {
        FAILED,
        NO_DELIMITER,
        COMMA_DELIMITER,
        SEMICOLON_DELIMITER
    }

    /*
     * enumEntry
     *   : modifiers SimpleName ("(" arguments ")")? classBody?
     *   ;
     */
    private ParseEnumEntryResult parseEnumEntry() {
        PsiBuilder.Marker entry = mark();

        parseModifierListWithStopAt(TokenSet.create(COMMA, SEMICOLON, RBRACE), ONLY_ESCAPED_REGULAR_ANNOTATIONS);

        if (!atSet(SOFT_KEYWORDS_AT_MEMBER_START) && at(IDENTIFIER)) {
            PsiBuilder.Marker nameAsDeclaration = mark();
            advance(); // IDENTIFIER
            nameAsDeclaration.done(OBJECT_DECLARATION_NAME);

            if (at(LPAR)) {
                // Arguments should be parsed here
                // Also, "fake" constructor call tree is created,
                // with empty type name inside
                PsiBuilder.Marker initializerList = mark();
                PsiBuilder.Marker delegatorSuperCall = mark();

                PsiBuilder.Marker callee = mark();
                PsiBuilder.Marker typeReference = mark();
                PsiBuilder.Marker type = mark();
                PsiBuilder.Marker referenceExpr = mark();
                referenceExpr.done(ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION);
                type.done(USER_TYPE);
                typeReference.done(TYPE_REFERENCE);
                callee.done(CONSTRUCTOR_CALLEE);

                myExpressionParsing.parseValueArgumentList();
                delegatorSuperCall.done(DELEGATOR_SUPER_CALL);
                initializerList.done(INITIALIZER_LIST);
            }
            if (at(LBRACE)) {
                parseClassBody();
            }
            boolean commaFound = at(COMMA);
            if (commaFound) {
                advance();
            }
            boolean semicolonFound = at(SEMICOLON);
            if (semicolonFound) {
                advance();
            }

            // Probably some helper function
            closeDeclarationWithCommentBinders(entry, ENUM_ENTRY, true);
            return semicolonFound
                   ? ParseEnumEntryResult.SEMICOLON_DELIMITER
                   : (commaFound ? ParseEnumEntryResult.COMMA_DELIMITER : ParseEnumEntryResult.NO_DELIMITER);
        }
        else {
            entry.rollbackTo();
            return ParseEnumEntryResult.FAILED;
        }
    }

    /*
     * classBody
     *   : ("{" members "}")?
     *   ;
     */
    private void parseClassBody() {
        PsiBuilder.Marker body = mark();

        myBuilder.enableNewlines();

        if (expect(LBRACE, "Expecting a class body")) {
            parseMembers();
            expect(RBRACE, "Missing '}");
        }

        myBuilder.restoreNewlinesState();

        body.done(CLASS_BODY);
    }

    /**
     * members
     *   : memberDeclaration*
     *   ;
     */
    private void parseMembers() {
        while (!eof() && !at(RBRACE)) {
            parseMemberDeclaration();
        }
    }

    /*
     * memberDeclaration
     *   : modifiers memberDeclaration'
     *   ;
     *
     * memberDeclaration'
     *   : companionObject
     *   : secondaryConstructor
     *   : function
     *   : property
     *   : class
     *   : extension
     *   : typeAlias
     *   : anonymousInitializer
     *   : object
     *   ;
     */
    private void parseMemberDeclaration() {
        if (at(SEMICOLON)) {
            advance(); // SEMICOLON
            return;
        }
        PsiBuilder.Marker decl = mark();

        ModifierDetector detector = new ModifierDetector();
        parseModifierList(detector, ALLOW_UNESCAPED_REGULAR_ANNOTATIONS_AT_MEMBER_MODIFIER_LIST);

        IElementType declType = parseMemberDeclarationRest(detector.isEnumDetected(), detector.isDefaultDetected());

        if (declType == null) {
            errorWithRecovery("Expecting member declaration", TokenSet.EMPTY);
            decl.drop();
        }
        else {
            closeDeclarationWithCommentBinders(decl, declType, true);
        }
    }

    private IElementType parseMemberDeclarationRest(boolean isEnum, boolean isDefault) {
        IElementType keywordToken = tt();
        IElementType declType = null;
        if (keywordToken == CLASS_KEYWORD || keywordToken == INTERFACE_KEYWORD) {
            declType = parseClass(isEnum, CLASS_MEMBER);
        }
        else if (keywordToken == FUN_KEYWORD) {
                declType = parseFunction();
        }
        else if (keywordToken == VAL_KEYWORD || keywordToken == VAR_KEYWORD) {
            declType = parseProperty();
        }
        else if (keywordToken == TYPE_ALIAS_KEYWORD) {
            declType = parseTypeAlias();
        }
        else if (keywordToken == OBJECT_KEYWORD) {
            parseObject(isDefault ? NameParsingMode.ALLOWED : NameParsingMode.REQUIRED, true, CLASS_MEMBER);
            declType = OBJECT_DECLARATION;
        }
        else if (at(INIT_KEYWORD)) {
            advance(); // init
            if (at(LBRACE)) {
                parseBlock();
            }
            else {
                mark().error("Expecting '{' after 'init'");
            }
            declType = ANONYMOUS_INITIALIZER;
        }
        else if (at(CONSTRUCTOR_KEYWORD)) {
            parseSecondaryConstructor();
            declType = SECONDARY_CONSTRUCTOR;
        }
        else if (at(LBRACE)) {
            error("Expecting member declaration");
            parseBlock();
            declType = FUN;
        }
        return declType;
    }

    /*
     * secondaryConstructor
     *   : modifiers "constructor" valueParameters (":" constructorDelegationCall)? block
     * constructorDelegationCall
     *   : "this" valueArguments
     *   : "super" valueArguments
     */
    private void parseSecondaryConstructor() {
        assert _at(CONSTRUCTOR_KEYWORD);

        advance(); // CONSTRUCTOR_KEYWORD

        TokenSet valueArgsRecoverySet = TokenSet.create(LBRACE, SEMICOLON, RPAR, EOL_OR_SEMICOLON, RBRACE);
        if (at(LPAR)) {
            parseValueParameterList(false, /*typeRequired = */ true, valueArgsRecoverySet);
        }
        else {
            errorWithRecovery("Expecting '('", TokenSet.orSet(valueArgsRecoverySet, TokenSet.create(COLON)));
        }

        if (at(COLON)) {
            advance(); // COLON

            PsiBuilder.Marker delegationCall = mark();

            if (at(THIS_KEYWORD) || at(SUPER_KEYWORD)) {
                parseThisOrSuper();
                myExpressionParsing.parseValueArgumentList();
            }
            else {
                error("Expecting a 'this' or 'super' constructor call");
                PsiBuilder.Marker beforeWrongDelegationCallee = null;
                if (!at(LPAR)) {
                    beforeWrongDelegationCallee = mark();
                    advance(); // wrong delegation callee
                }
                myExpressionParsing.parseValueArgumentList();

                if (beforeWrongDelegationCallee != null) {
                    if (at(LBRACE)) {
                        beforeWrongDelegationCallee.drop();
                    }
                    else {
                        beforeWrongDelegationCallee.rollbackTo();
                    }
                }
            }

            delegationCall.done(CONSTRUCTOR_DELEGATION_CALL);
        }
        else {
            // empty constructor delegation call
            PsiBuilder.Marker emptyDelegationCall = mark();
            mark().done(CONSTRUCTOR_DELEGATION_REFERENCE);
            emptyDelegationCall.done(CONSTRUCTOR_DELEGATION_CALL);
        }

        if (at(LBRACE)) {
            parseBlock();
        }
    }

    private void parseThisOrSuper() {
        assert _at(THIS_KEYWORD) || _at(SUPER_KEYWORD);
        PsiBuilder.Marker mark = mark();

        advance(); // THIS_KEYWORD | SUPER_KEYWORD

        mark.done(CONSTRUCTOR_DELEGATION_REFERENCE);
    }

    /*
     * initializer
     *   : annotations constructorInvocation // type parameters may (must?) be omitted
     *   ;
     */
    private void parseInitializer() {
        PsiBuilder.Marker initializer = mark();
        parseAnnotations(ONLY_ESCAPED_REGULAR_ANNOTATIONS);

        IElementType type;
        if (atSet(TYPE_REF_FIRST)) {
            PsiBuilder.Marker reference = mark();
            parseTypeRef();
            reference.done(CONSTRUCTOR_CALLEE);
            type = DELEGATOR_SUPER_CALL;
        }
        else {
            errorWithRecovery("Expecting constructor call (<class-name>(...))",
                              TokenSet.orSet(TOP_LEVEL_DECLARATION_FIRST, TokenSet.create(RBRACE, LBRACE, COMMA, SEMICOLON)));
            initializer.drop();
            return;
        }
        myExpressionParsing.parseValueArgumentList();

        initializer.done(type);
    }

    /*
     * typeAlias
     *   : modifiers "typealias" SimpleName (typeParameters typeConstraints)? "=" type
     *   ;
     */
    JetNodeType parseTypeAlias() {
        assert _at(TYPE_ALIAS_KEYWORD);

        advance(); // TYPE_ALIAS_KEYWORD

        expect(IDENTIFIER, "Type name expected", TokenSet.orSet(TokenSet.create(LT, EQ, SEMICOLON), TOP_LEVEL_DECLARATION_FIRST));

        if (parseTypeParameterList(TYPE_PARAMETER_GT_RECOVERY_SET)) {
            parseTypeConstraints();
        }

        expect(EQ, "Expecting '='", TokenSet.orSet(TOP_LEVEL_DECLARATION_FIRST, TokenSet.create(SEMICOLON)));

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

        PsiBuilder.Marker receiver = mark();
        boolean receiverTypeDeclared = parseReceiverType("property", propertyNameFollow);

        boolean multiDeclaration = at(LPAR);

        errorIf(receiver, multiDeclaration && receiverTypeDeclared, "Receiver type is not allowed on a multi-declaration");

        if (multiDeclaration) {
            PsiBuilder.Marker multiDecl = mark();
            parseMultiDeclarationName(propertyNameFollow);
            errorIf(multiDecl, !local, "Multi-declarations are only allowed for local variables/values");
        }
        else {
            parseFunctionOrPropertyName(receiverTypeDeclared, "property", propertyNameFollow, /*nameRequired = */ false);
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
                    errorUntil("Property getter or setter expected", TokenSet.create(EOL_OR_SEMICOLON, LBRACE, RBRACE));
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

                parseModifierListWithUnescapedAnnotations(TokenSet.create(COMMA, RPAR, COLON, IN_KEYWORD, EQ));

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

        parseModifierList(ONLY_ESCAPED_REGULAR_ANNOTATIONS);

        if (!at(GET_KEYWORD) && !at(SET_KEYWORD)) {
            getterOrSetter.rollbackTo();
            return false;
        }

        boolean setter = at(SET_KEYWORD);
        advance(); // GET_KEYWORD or SET_KEYWORD

        if (!at(LPAR)) {
            // Account for Jet-114 (val a : int get {...})
            TokenSet ACCESSOR_FIRST_OR_PROPERTY_END = TokenSet.orSet(MODIFIER_KEYWORDS, TokenSet.create(AT, GET_KEYWORD, SET_KEYWORD, EOL_OR_SEMICOLON, RBRACE));
            if (!atSet(ACCESSOR_FIRST_OR_PROPERTY_END)) {
                errorUntil("Accessor body expected", TokenSet.orSet(ACCESSOR_FIRST_OR_PROPERTY_END, TokenSet.create(LBRACE, LPAR, EQ)));
            }
            else {
                closeDeclarationWithCommentBinders(getterOrSetter, PROPERTY_ACCESSOR, false);
                return true;
            }
        }

        myBuilder.disableNewlines();
        expect(LPAR, "Expecting '('", TokenSet.create(RPAR, IDENTIFIER, COLON, LBRACE, EQ));
        if (setter) {
            PsiBuilder.Marker parameterList = mark();
            PsiBuilder.Marker setterParameter = mark();
            parseModifierListWithUnescapedAnnotations(TokenSet.create(RPAR, COMMA, COLON));
            expect(IDENTIFIER, "Expecting parameter name", TokenSet.create(RPAR, COLON, LBRACE, EQ));

            if (at(COLON)) {
                advance();  // COLON
                parseTypeRef();
            }
            setterParameter.done(VALUE_PARAMETER);
            parameterList.done(VALUE_PARAMETER_LIST);
        }
        if (!at(RPAR)) {
            errorUntil("Expecting ')'", TokenSet.create(RPAR, COLON, LBRACE, RBRACE, EQ, EOL_OR_SEMICOLON));
        }
        if (at(RPAR)) {
            advance();
        }
        myBuilder.restoreNewlinesState();

        if (at(COLON)) {
            advance();

            parseTypeRef();
        }

        parseFunctionBody();

        closeDeclarationWithCommentBinders(getterOrSetter, PROPERTY_ACCESSOR, false);

        return true;
    }

    /*
     * function
     *   : modifiers "fun" typeParameters?
     *       (type "." | annotations)?
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
            parseTypeParameterList(TokenSet.create(LBRACKET, LBRACE, RBRACE, LPAR));
            typeParameterListOccurred = true;
        }

        myBuilder.disableJoiningComplexTokens();

        TokenSet functionNameFollow = TokenSet.create(LT, LPAR, RPAR, COLON, EQ);
        boolean receiverFound = parseReceiverType("function", functionNameFollow);

        // function as expression has no name
        parseFunctionOrPropertyName(receiverFound, "function", functionNameFollow, /*nameRequired = */ true);

        myBuilder.restoreJoiningComplexTokensState();

        TokenSet valueParametersFollow = TokenSet.create(EQ, LBRACE, RBRACE, SEMICOLON, RPAR);

        if (at(LT)) {
            PsiBuilder.Marker error = mark();
            parseTypeParameterList(TokenSet.orSet(TokenSet.create(LPAR), valueParametersFollow));
            errorIf(error, typeParameterListOccurred, "Only one type parameter list is allowed for a function");
            typeParameterListOccurred = true;
        }

        if (at(LPAR)) {
            parseValueParameterList(false, /* typeRequired  = */ false, valueParametersFollow);
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
     *   (type "." | annotations)?
     */
    private boolean parseReceiverType(String title, TokenSet nameFollow) {
        PsiBuilder.Marker annotations = mark();
        boolean annotationsPresent = parseAnnotations(ONLY_ESCAPED_REGULAR_ANNOTATIONS);
        int lastDot = lastDotAfterReceiver();
        boolean receiverPresent = lastDot != -1;
        if (annotationsPresent) {
            if (receiverPresent) {
                annotations.rollbackTo();
            }
            else {
                annotations.error("Annotations are not allowed in this position");
            }
        }
        else {
            annotations.drop();
        }

        if (!receiverPresent) return false;

        createTruncatedBuilder(lastDot).parseTypeRef();

        if (atSet(RECEIVER_TYPE_TERMINATORS)) {
            advance(); // expectation
        }
        else {
            errorWithRecovery("Expecting '.' before a " + title + " name", nameFollow);
        }
        return true;
    }

    private int lastDotAfterReceiver() {
        if (at(LPAR)) {
            return matchTokenStreamPredicate(
                    new FirstBefore(
                            new AtSet(RECEIVER_TYPE_TERMINATORS),
                            new AbstractTokenStreamPredicate() {
                                @Override
                                public boolean matching(boolean topLevel) {
                                    if (topLevel && definitelyOutOfReceiver()) {
                                        return true;
                                    }
                                    return topLevel && !at(QUEST) && !at(LPAR) && !at(RPAR);
                                }
                            }
                    ));
        }
        else {
            return matchTokenStreamPredicate(
                    new LastBefore(
                            new AtSet(RECEIVER_TYPE_TERMINATORS),
                            new AbstractTokenStreamPredicate() {
                                @Override
                                public boolean matching(boolean topLevel) {
                                    if (topLevel && (definitelyOutOfReceiver() || at(LPAR))) return true;
                                    if (topLevel && at(IDENTIFIER)) {
                                        IElementType lookahead = lookahead(1);
                                        return lookahead != LT && lookahead != DOT && lookahead != SAFE_ACCESS && lookahead != QUEST;
                                    }
                                    return false;
                                }
                            }));
        }
    }

    private boolean definitelyOutOfReceiver() {
        return atSet(EQ, COLON, LBRACE, RBRACE, BY_KEYWORD) || atSet(TOP_LEVEL_DECLARATION_FIRST);
    }

    /*
     * IDENTIFIER
     */
    private void parseFunctionOrPropertyName(boolean receiverFound, String title, TokenSet nameFollow, boolean nameRequired) {
        if (nameRequired && atSet(nameFollow)) return; // no name

        TokenSet recoverySet = TokenSet.orSet(nameFollow, TokenSet.create(LBRACE, RBRACE));
        if (!receiverFound) {
            expect(IDENTIFIER, "Expecting " + title + " name or receiver type", recoverySet);
        }
        else {
            expect(IDENTIFIER, "Expecting " + title + " name", recoverySet);
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
            error("Expecting function body");
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

        expect(RBRACE, "Expecting '}'");
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
     *   : annotations SimpleName ":" type
     *   ;
     */
    private void parseTypeConstraint() {
        PsiBuilder.Marker constraint = mark();

        parseAnnotations(ONLY_ESCAPED_REGULAR_ANNOTATIONS);

        PsiBuilder.Marker reference = mark();
        if (expect(IDENTIFIER, "Expecting type parameter name", TokenSet.orSet(TokenSet.create(COLON, COMMA, LBRACE, RBRACE), TYPE_REF_FIRST))) {
            reference.done(REFERENCE_EXPRESSION);
        }
        else {
            reference.drop();
        }

        expect(COLON, "Expecting ':' before the upper bound", TokenSet.orSet(TokenSet.create(LBRACE, RBRACE), TYPE_REF_FIRST));

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

        parseModifierListWithUnescapedAnnotations(TokenSet.create(COMMA, GT, COLON));

        expect(IDENTIFIER, "Type parameter name expected", TokenSet.EMPTY);

        if (at(COLON)) {
            advance(); // COLON
            parseTypeRef();
        }

        mark.done(TYPE_PARAMETER);

    }

    /*
     * type
     *   : annotations typeDescriptor
     *
     * typeDescriptor
     *   : selfType
     *   : functionType
     *   : userType
     *   : nullableType
     *   : "dynamic"
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
        PsiBuilder.Marker typeRefMarker = mark();
        parseAnnotations(ONLY_ESCAPED_REGULAR_ANNOTATIONS);

        PsiBuilder.Marker typeElementMarker = mark();

        IElementType lookahead = lookahead(1);
        IElementType lookahead2 = lookahead(2);
        boolean typeBeforeDot = true;
        if (at(IDENTIFIER) && !(lookahead == DOT && lookahead2 == IDENTIFIER) && lookahead != LT && at(DYNAMIC_KEYWORD)) {
            PsiBuilder.Marker dynamicType = mark();
            advance(); // DYNAMIC_KEYWORD
            dynamicType.done(DYNAMIC_TYPE);
        }
        else if (at(IDENTIFIER) || at(PACKAGE_KEYWORD) || atParenthesizedMutableForPlatformTypes(0)) {
            parseUserType();
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
                    TokenSet.orSet(TOP_LEVEL_DECLARATION_FIRST,
                                   TokenSet.create(EQ, COMMA, GT, RBRACKET, DOT, RPAR, RBRACE, LBRACE, SEMICOLON),
                                   extraRecoverySet));
            typeBeforeDot = false;
        }

        // Disabling token merge is required for cases like
        //    Int?.(Foo) -> Bar
        myBuilder.disableJoiningComplexTokens();
        typeElementMarker = parseNullableTypeSuffix(typeElementMarker);
        myBuilder.restoreJoiningComplexTokensState();

        if (typeBeforeDot && at(DOT)) {
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

        typeElementMarker.drop();
        return typeRefMarker;
    }

    @NotNull
    PsiBuilder.Marker parseNullableTypeSuffix(@NotNull PsiBuilder.Marker typeElementMarker) {
        // ?: is joined regardless of joining state
        while (at(QUEST) && myBuilder.rawLookup(1) != COLON) {
            PsiBuilder.Marker precede = typeElementMarker.precede();
            advance(); // QUEST
            typeElementMarker.done(NULLABLE_TYPE);
            typeElementMarker = precede;
        }
        return typeElementMarker;
    }

    /*
     * userType
     *   : ("package" ".")? simpleUserType{"."}
     *   ;
     *
     *   recovers on platform types:
     *    - Foo!
     *    - (Mutable)List<Foo>!
     *    - Array<(out) Foo>!
     */
    void parseUserType() {
        PsiBuilder.Marker userType = mark();

        if (at(PACKAGE_KEYWORD)) {
            advance(); // PACKAGE_KEYWORD
            expect(DOT, "Expecting '.'", TokenSet.create(IDENTIFIER, LBRACE, RBRACE));
        }

        PsiBuilder.Marker reference = mark();
        while (true) {
            recoverOnParenthesizedWordForPlatformTypes(0, "Mutable", true);

            if (expect(IDENTIFIER, "Expecting type name",
                       TokenSet.orSet(JetExpressionParsing.EXPRESSION_FIRST, JetExpressionParsing.EXPRESSION_FOLLOW, DECLARATION_FIRST))) {
                reference.done(REFERENCE_EXPRESSION);
            }
            else {
                reference.drop();
                break;
            }

            parseTypeArgumentList();

            recoverOnPlatformTypeSuffix();

            if (!at(DOT)) {
                break;
            }
            if (lookahead(1) == LPAR && !atParenthesizedMutableForPlatformTypes(1)) {
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

    private boolean atParenthesizedMutableForPlatformTypes(int offset) {
        return recoverOnParenthesizedWordForPlatformTypes(offset, "Mutable", false);
    }

    private boolean recoverOnParenthesizedWordForPlatformTypes(int offset, String word, boolean consume) {
        // Array<(out) Foo>! or (Mutable)List<Bar>!
        if (lookahead(offset) == LPAR && lookahead(offset + 1) == IDENTIFIER && lookahead(offset + 2) == RPAR && lookahead(offset + 3) == IDENTIFIER) {
            PsiBuilder.Marker error = mark();

            advance(offset);

            advance(); // LPAR
            if (!word.equals(myBuilder.getTokenText())) {
                // something other than "out" / "Mutable"
                error.rollbackTo();
                return false;
            }
            else {
                advance(); // IDENTIFIER('out')
                advance(); // RPAR

                if (consume) {
                    error.error("Unexpected tokens");
                }
                else {
                    error.rollbackTo();
                }

                return true;
            }
        }
        return false;
    }

    private void recoverOnPlatformTypeSuffix() {
        // Recovery for platform types
        if (at(EXCL)) {
            PsiBuilder.Marker error = mark();
            advance(); // EXCL
            error.error("Unexpected token");
        }
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

            recoverOnParenthesizedWordForPlatformTypes(0, "out", true);

//            TokenSet lookFor = TokenSet.create(IDENTIFIER);
//            TokenSet stopAt = TokenSet.create(COMMA, COLON, GT);
//            parseModifierListWithUnescapedAnnotations(MODIFIER_LIST, lookFor, stopAt);
            // Currently we do not allow annotations
            parseModifierList(NO_ANNOTATIONS);

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

    public void parseModifierListWithUnescapedAnnotations(TokenSet stopAt) {
        parseModifierListWithLookForStopAt(TokenSet.create(IDENTIFIER), stopAt, ALLOW_UNESCAPED_REGULAR_ANNOTATIONS);
    }

    public void parseModifierListWithStopAt(TokenSet stopAt, AnnotationParsingMode mode) {
        parseModifierListWithLookForStopAt(TokenSet.create(IDENTIFIER), stopAt, mode);
    }

    public void parseModifierListWithUnescapedAnnotations(TokenSet lookFor, TokenSet stopAt) {
        parseModifierListWithLookForStopAt(lookFor, stopAt, ALLOW_UNESCAPED_REGULAR_ANNOTATIONS);
    }

    public void parseModifierListWithLookForStopAt(TokenSet lookFor, TokenSet stopAt, AnnotationParsingMode mode) {
        int lastId = matchTokenStreamPredicate(new LastBefore(new AtSet(lookFor), new AnnotationTargetStop(stopAt, ANNOTATION_TARGETS), false));
        createTruncatedBuilder(lastId).parseModifierList(mode);
    }

    private class AnnotationTargetStop extends AbstractTokenStreamPredicate {
        private final TokenSet stopAt;
        private final TokenSet annotationTargets;

        private IElementType previousToken;
        private IElementType tokenBeforePrevious;

        public AnnotationTargetStop(TokenSet stopAt, TokenSet annotationTargets) {
            this.stopAt = stopAt;
            this.annotationTargets = annotationTargets;
        }

        @Override
        public boolean matching(boolean topLevel) {
            if (atSet(stopAt)) return true;

            if (at(COLON) && !(tokenBeforePrevious == AT && (previousToken == IDENTIFIER || annotationTargets.contains(previousToken)))) {
                return true;
            }

            tokenBeforePrevious = previousToken;
            previousToken = tt();

            return false;
        }
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

        parseValueParameterList(true, /* typeRequired  = */ true, TokenSet.EMPTY);

        expect(ARROW, "Expecting '->' to specify return type of a function type", TYPE_REF_FIRST);
        parseTypeRef();

        return functionType;
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
    void parseValueParameterList(boolean isFunctionTypeContents, boolean typeRequired, TokenSet recoverySet) {
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
                    if (!tryParseValueParameter(typeRequired)) {
                        PsiBuilder.Marker valueParameter = mark();
                        parseModifierList(ONLY_ESCAPED_REGULAR_ANNOTATIONS); // lazy, out, ref
                        parseTypeRef();
                        closeDeclarationWithCommentBinders(valueParameter, VALUE_PARAMETER, false);
                    }
                }
                else {
                    parseValueParameter(typeRequired);
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
    private boolean tryParseValueParameter(boolean typeRequired) {
        return parseValueParameter(true, typeRequired);
    }

    public void parseValueParameter(boolean typeRequired) {
        parseValueParameter(false, typeRequired);
    }

    private boolean parseValueParameter(boolean rollbackOnFailure, boolean typeRequired) {
        PsiBuilder.Marker parameter = mark();

        parseModifierListWithUnescapedAnnotations(TokenSet.create(COMMA, RPAR));

        if (at(VAR_KEYWORD) || at(VAL_KEYWORD)) {
            advance(); // VAR_KEYWORD | VAL_KEYWORD
        }

        if (!parseFunctionParameterRest(typeRequired) && rollbackOnFailure) {
            parameter.rollbackTo();
            return false;
        }

        closeDeclarationWithCommentBinders(parameter, VALUE_PARAMETER, false);
        return true;
    }

    /*
     * functionParameterRest
     *   : parameter ("=" element)?
     *   ;
     */
    private boolean parseFunctionParameterRest(boolean typeRequired) {
        boolean noErrors = true;

        // Recovery for the case 'fun foo(Array<String>) {}'
        // Recovery for the case 'fun foo(: Int) {}'
        if ((at(IDENTIFIER) && lookahead(1) == LT) || at(COLON)) {
            error("Parameter name expected");
            if (at(COLON)) {
                // We keep noErrors == true so that unnamed parameters starting with ":" are not rolled back during parsing of functional types
                advance(); // COLON
            }
            else {
                noErrors = false;
            }
            parseTypeRef();
        }
        else {
            expect(IDENTIFIER, "Parameter name expected", PARAMETER_NAME_RECOVERY_SET);

            if (at(COLON)) {
                advance(); // COLON
                parseTypeRef();
            }
            else if (typeRequired) {
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

    /*package*/ static class ModifierDetector implements Consumer<IElementType> {
        private boolean enumDetected = false;
        private boolean defaultDetected = false;

        @Override
        public void consume(IElementType item) {
            if (item == JetTokens.ENUM_KEYWORD) {
                enumDetected = true;
            }
            else if (item == JetTokens.COMPANION_KEYWORD) {
                defaultDetected = true;
            }
        }

        public boolean isEnumDetected() {
            return enumDetected;
        }

        public boolean isDefaultDetected() {
            return defaultDetected;
        }
    }

    enum AnnotationParsingMode {
        FILE_ANNOTATIONS_BEFORE_PACKAGE(false, true, false, true),
        FILE_ANNOTATIONS_WHEN_PACKAGE_OMITTED(false, true, false, true),
        ONLY_ESCAPED_REGULAR_ANNOTATIONS(false, false, false, true),
        ALLOW_UNESCAPED_REGULAR_ANNOTATIONS(true, false, false, true),
        ALLOW_UNESCAPED_REGULAR_ANNOTATIONS_AT_MEMBER_MODIFIER_LIST(true, false, true, true),
        PRIMARY_CONSTRUCTOR_MODIFIER_LIST(true, false, false, true),
        PRIMARY_CONSTRUCTOR_MODIFIER_LIST_LOCAL(false, false, false, true),
        NO_ANNOTATIONS(false, false, false, false);


        boolean allowShortAnnotations;
        boolean isFileAnnotationParsingMode;
        boolean atMemberStart;
        boolean allowAnnotations;

        AnnotationParsingMode(
                boolean allowShortAnnotations,
                boolean isFileAnnotationParsingMode,
                boolean atMemberStart,
                boolean allowAnnotations
        ) {
            this.allowShortAnnotations = allowShortAnnotations;
            this.isFileAnnotationParsingMode = isFileAnnotationParsingMode;
            this.atMemberStart = atMemberStart;
            this.allowAnnotations = allowAnnotations;
        }
    }
}
