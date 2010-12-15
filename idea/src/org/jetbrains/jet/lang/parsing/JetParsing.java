/*
 * @author max
 */
package org.jetbrains.jet.lang.parsing;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.WhitespaceSkippedCallback;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.jet.JetNodeType;
import org.jetbrains.jet.lexer.JetSoftKeywordToken;
import org.jetbrains.jet.lexer.JetToken;

import static org.jetbrains.jet.JetNodeTypes.*;
import static org.jetbrains.jet.lexer.JetTokens.*;

public class JetParsing {
    public static final TokenSet CLASS_NAME_RECOVERY_SET = TokenSet.create(LT, WRAPS_KEYWORD, LPAR, COLON, LBRACE);
    public static final TokenSet TYPE_PARAMETER_GT_RECOVERY_SET = TokenSet.create(WHERE_KEYWORD, WRAPS_KEYWORD, LPAR, COLON, LBRACE, GT);
    public static final TokenSet PARAMETER_NAME_RECOVERY_SET = TokenSet.create(COLON, EQ, COMMA, RPAR);
    private static final TokenSet NAMESPACE_NAME_RECOVERY_SET = TokenSet.create(DOT, EOL_OR_SEMICOLON);

    private final WhitespaceSkippedCallback myWhitespaceSkippedCallback = new WhitespaceSkippedCallback() {
        public void onSkip(IElementType type, int start, int end) {
            CharSequence whitespace = JetParsing.this.myBuilder.getOriginalText();
            for (int i = start; i < end; i++) {
                char c = whitespace.charAt(i);
                if (c == '\n') {
                    myEOLInLastWhitespace = true;
                    break;
                }
            }
        }
    };
    private final PsiBuilder myBuilder;
    private boolean myEOLInLastWhitespace;

    public JetParsing(PsiBuilder myBuilder) {
        this.myBuilder = myBuilder;
        myBuilder.setWhitespaceSkippedCallback(myWhitespaceSkippedCallback);
    }

    /*
     * [start] jetlFile
     *   : preamble toplevelObject[| import]* [eof]
     *   ;
     */
    public void parseFile() {
        PsiBuilder.Marker fileMarker = mark();
        PsiBuilder.Marker namespaceMarker = mark();

        parsePreamble();

        parseToplevelDeclarations(false);

        namespaceMarker.done(NAMESPACE);
        fileMarker.done(JET_FILE);
    }

    /*
     * toplevelObject[| import]*
     */
    private void parseToplevelDeclarations(boolean insideBlock) {
        while (!eof() && (!insideBlock || !at(RBRACE))) {
            if (at(IMPORT_KEYWORD)) {
                parseImportDirective();
            }
            else {
                parseTopLevelObject();
            }
        }
    }

    /*
     *preamble
     *  : namespaceHeader? import*
     *  ;
     */
    private void parsePreamble() {
        /*
         * namespaceHeader
         *   : "namespace" SimpleName{"."}
         *   ;
         */
        if (at(NAMESPACE_KEYWORD)) {
            advance(); // NAMESPACE_KEYWORD

            parseNamespaceName();
        }

        while (at(IMPORT_KEYWORD)) {
            parseImportDirective();
        }
    }

    /* SimpleName{"."} */
    private void parseNamespaceName() {
        PsiBuilder.Marker nsName = mark();
        expect(IDENTIFIER, "Expecting qualified name", NAMESPACE_NAME_RECOVERY_SET);
        while (at(DOT)) {
            advance(); // DOT
            expect(IDENTIFIER, "Namespace name must be a '.'-separated identifier list", NAMESPACE_NAME_RECOVERY_SET);
            if (at(EOL_OR_SEMICOLON)) break;
        }
        nsName.done(NAMESPACE_NAME);
    }

    /*
     * import
     *   : "import" SimpleName{"."} ("." "*" | "as" SimpleName)?
     *   ;
     */
    private void parseImportDirective() {
        assert at(IMPORT_KEYWORD);
        PsiBuilder.Marker importDirective = mark();
        advance(); // IMPORT_KEYWORD

        expect(IDENTIFIER, "Expecting qualified name", TokenSet.create(DOT, MAP));
        while (at(DOT)) {
            advance(); // DOT
            if (at(IDENTIFIER)) {
                advance(); // IDENTIFIER
            }
            else if (at(MUL)) {
                advance(); // MUL
                handleUselessRename();
                break;
            }
            else {
                errorWithRecovery("Qualified name must be a '.'-separated identifier list", TokenSet.create(AS_KEYWORD, DOT, MAP, SEMICOLON));
            }
        }
        if (at(MAP)) {
            advance(); // MAP
            handleUselessRename();
        }
        else if (at(AS_KEYWORD)) {
            advance(); // AS_KEYWORD
            expect(IDENTIFIER, "Expecting identifier", TokenSet.create(SEMICOLON));
        }
        cosumeIf(SEMICOLON);
        importDirective.done(IMPORT_DIRECTIVE);
    }

    private void handleUselessRename() {
        if (at(AS_KEYWORD)) {
            PsiBuilder.Marker as = mark();
            advance(); // AS_KEYWORD
            cosumeIf(IDENTIFIER);
            as.error("Cannot rename a all imported items to one identifier");
        }
    }

    /*
     * toplevelObject
     *   : namespace
     *   : class
     *   : extension
     *   : function
     *   : property
     *   : typedef
     *   ;
     */
    private void parseTopLevelObject() {
        PsiBuilder.Marker decl = mark();
        if (!parseModifierList()) {
            decl.drop();
            advance(); // TODO
            return;
        }

        JetToken keywordToken = tt();
        JetNodeType declType = null;
        if (keywordToken == NAMESPACE_KEYWORD) {
            declType = parseNamespaceBlock();
        }
        else if (keywordToken == CLASS_KEYWORD) {
            declType = parseClass();
        }
        else if (keywordToken == EXTENSION_KEYWORD) {
            declType = parseExtension();
        }
        else if (keywordToken == FUN_KEYWORD) {
            declType = parseFunction();
        }
        else if (keywordToken == VAL_KEYWORD || keywordToken == VAR_KEYWORD) {
            declType = parseProperty();
        }
        else if (keywordToken == TYPE_KEYWORD) {
            declType = parseTypedef();
        }

        if (declType == null) {
            errorAndAdvance("Expecting namespace or top level declaration");
            decl.drop();
        }
        else {
            decl.done(declType);
        }
    }

    private boolean parseModifierList() {
        // TODO
        return true;
    }

    /*
     * namespace
     *   : "namespace" SimpleName{"."} "{"
     *        import*
     *        toplevelObject[| import]*
     *     "}"
     *   ;
     */
    private JetNodeType parseNamespaceBlock() {
        assert at(NAMESPACE);
        advance(); // NAMESPACE

        if (at(LBRACE)) {
            error("Expecting namespace name");
        }
        else {
            parseNamespaceName();
        }

        if (!at(LBRACE)) {
            error("A namespace block in '{...}' expected");
            return NAMESPACE;
        }

        advance(); // LBRACE
        PsiBuilder.Marker namespaceBody = mark();

        parseToplevelDeclarations(true);

        namespaceBody.done(NAMESPACE_BODY);
        expect(RBRACE, "Expecting '}'");

        return NAMESPACE;
    }

    private JetNodeType parseClass() {
        assert at(CLASS_KEYWORD);
        advance(); // CLASS_KEYWORD

        expect(IDENTIFIER, "Class name expected", CLASS_NAME_RECOVERY_SET);
        parseTypeParameterList();

        cosumeIf(WRAPS_KEYWORD);
        if (at(LPAR)) {
            parsePrimaryConstructorParameterList();
        }

        if (at(COLON)) {
            advance(); // COLON
            parseDelegationSpecifierList();
        }

        if (at(LBRACE)) {
            parseClassBody();
        }

        return CLASS;
    }

    private JetNodeType parseTypedef() {
        advance(); // TODO
        return TYPEDEF;
    }

    private JetNodeType parseProperty() {
        advance(); // TODO
        return PROPERTY;
    }

    private JetNodeType parseFunction() {
        advance(); // TODO
        return FUN;
    }

    private JetNodeType parseExtension() {
        advance(); // TODO
        return EXTENSION;
    }

    private void parseClassBody() {
        assert at(LBRACE);
        PsiBuilder.Marker body = mark();
        advance(); // LBRACE

        while (!eof()) {
            if (at(RBRACE)) {
                break;
            }
            advance(); // TODO
        }
        expect(RBRACE, "Missing '}");
        body.done(CLASS_BODY);
    }

    private void parseDelegationSpecifierList() {
        PsiBuilder.Marker list = mark();
        while (true) {
            parseDelegationSpecifier();
            if (!at(COMMA)) break;
            advance(); // COMMA
        }

        list.done(DELEGATION_SPECIFIER_LIST);
    }

    private void parseDelegationSpecifier() {
        PsiBuilder.Marker specifier = mark();
        parseAttributeList();

        PsiBuilder.Marker delegator = mark();
        parseTypeRef(); // TODO: Error recovery!!!
        if (at(BY_KEYWORD)) {
            advance(); // BY_KEYWORD
            parseExpression();
            delegator.done(DELEGATOR_BY);
        }
        else if (at(LPAR)) {
            parseValueParameterList();
            delegator.done(DELEGATOR_SUPER_CALL);
        }
        else {
            delegator.done(DELEGATOR_SUPER_CLASS);
        }

        specifier.done(DELEGATION_SPECIFIER);
    }

    private void parseValueParameterList() {
        assert at(LPAR);
        PsiBuilder.Marker list = mark();
        advance(); // LPAR
        while (true) {
            if (at(IDENTIFIER) && lookahead(1) == EQ) {
                PsiBuilder.Marker named = mark();
                advance(); // IDENTIFIER
                advance(); // EQ
                parseExpression();
                named.done(NAMED_ARGUMENT);
            }
            else {
                parseExpression();
            }

            if (!at(COMMA)) {
                break;
            }
        }

        expect(RPAR, "Missing ')'");

        list.done(VALUE_PARAMETER_LIST);

    }

    private void parseAttributeList() {
        // TODO
    }

    private void parsePrimaryConstructorParameterList() {
        assert at(LPAR);
        PsiBuilder.Marker cons = mark();
        advance(); // LPAR

        while (true) {
            parsePrimaryConstructorParameter();
            if (!at(COMMA)) break;
            advance(); // COMMA
        }

        expect(RPAR, "')' expected");

        cons.done(PRIMARY_CONSTRUCTOR_PARAMETERS_LIST);
    }

    private void parsePrimaryConstructorParameter() {
        PsiBuilder.Marker param = mark();
        parseModifierList();

        if (at(VAR_KEYWORD) || at(VAL_KEYWORD)) {
            advance(); // VAR_KEYWORD | VAL_KEYWORD
        }

        parseFunctionParameterRest();

        param.done(PRIMARY_CONSTRUCTOR_PARAMETER);
    }

    private void parseFunctionParameterRest() {
        expect(IDENTIFIER, "Parameter name expected", PARAMETER_NAME_RECOVERY_SET);

        if (at(COLON)) {
            advance(); // COLON
            parseTypeRef();
        }
        else {
            error("Parameters must have type annotation");
        }

        if (at(EQ)) {
            advance(); // EQ
            parseExpression();
        }
    }

    private void parseExpression() {
        advance(); // TODO
    }

    private void parseTypeParameterList() {
        PsiBuilder.Marker list = mark();
        if (tt() == LT) {
            advance(); // LT

            while (true) {
                parseTypeParameter();

                if (!at(COMMA)) break;
                advance(); // COMMA
            }

            expect(GT, "Missing '>'", TYPE_PARAMETER_GT_RECOVERY_SET);
        }
        list.done(TYPE_PARAMETER_LIST);
    }

    private void parseTypeParameter() {
        if (TYPE_PARAMETER_GT_RECOVERY_SET.contains(tt())) {
            error("Type parameter declaration expected");
            return;
        }

        PsiBuilder.Marker mark = mark();
        parseModifierList();
        expect(IDENTIFIER, "Type parameter name expected", TokenSet.EMPTY);

        if (at(COLON)) {
            advance(); // COLON
            parseTypeRef();
        }

        mark.done(TYPE_PARAMETER);

    }

    private void parseTypeRef() {
        advance(); // tODO:
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean expect(JetToken expectation, String message) {
        return expect(expectation, message, null);
    }

    private PsiBuilder.Marker mark() {
        return myBuilder.mark();
    }

    private void error(String message) {
        myBuilder.error(message);
    }

    private boolean expect(JetToken expectation, String message, TokenSet recoverySet) {
        if (at(expectation)) {
            advance(); // expectation
            return true;
        }

        errorWithRecovery(message, recoverySet);

        return false;
    }

    private void errorWithRecovery(String message, TokenSet recoverySet) {
        JetToken tt = tt();
        if (recoverySet == null || recoverySet.contains(tt)
                || (recoverySet.contains(EOL_OR_SEMICOLON)
                        && (tt == SEMICOLON || myEOLInLastWhitespace))) {
            error(message);
        }
        else {
            errorAndAdvance(message);
        }
    }

    private void errorAndAdvance(String message) {
        PsiBuilder.Marker err = mark();
        advance(); // erroneous token
        err.error(message);
    }

    private boolean eof() {
        return myBuilder.eof();
    }

    private void advance() {
        myEOLInLastWhitespace = false;
        myBuilder.advanceLexer();
    }

    private JetToken tt() {
        return (JetToken) myBuilder.getTokenType();
    }

    private boolean at(final IElementType expectation) {
        JetToken token = tt();
        if (token == expectation) return true;
        if (expectation == EOL_OR_SEMICOLON) {
            if (token == SEMICOLON) return true;
            if (myEOLInLastWhitespace) return true;
        }
        if (token == IDENTIFIER && expectation instanceof JetSoftKeywordToken) {
            if (((JetSoftKeywordToken) expectation).getValue().equals(myBuilder.getTokenText())) {
//                myBuilder.setTokenTypeRemapper(new ITokenTypeRemapper() {
//                    public IElementType filter(IElementType source, int start, int end, CharSequence text) {
//                        return expectation;
//                    }
//                });

//                tt();
//                myBuilder.setTokenTypeRemapper(null);
                myBuilder.remapCurrentToken(expectation);
                return true;
            }
        }
        return false;
    }

    private IElementType lookahead(int k) {
        PsiBuilder.Marker tmp = mark();
        for (int i = 0; i < k; i++) advance();

        JetToken tt = tt();
        tmp.rollbackTo();
        return tt;
    }

    private void cosumeIf(JetToken token) {
        if (at(token)) advance(); // token
    }

}
