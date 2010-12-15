/*
 * @author max
 */
package org.jetbrains.jet.lang.parsing;

import com.intellij.lang.ITokenTypeRemapper;
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

        while (!eof()) {
            if (at(IMPORT_KEYWORD)) {
                parseImportDirective();
            }
            else {
                parseTopLevelObject();
            }
        }

        namespaceMarker.done(NAMESPACE);
        fileMarker.done(JET_FILE);
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
            advance();

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
            advance();
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
        advance();

        expect(IDENTIFIER, "Expecting qualified name", TokenSet.create(DOT, MAP));
        while (at(DOT)) {
            advance();
            if (at(IDENTIFIER)) {
                advance();
            }
            else if (at(MUL)) {
                advance();
                handleUselessRename();
                break;
            }
            else {
                errorWithRecovery("Qualified name must be a '.'-separated identifier list", TokenSet.create(AS_KEYWORD, DOT, MAP, SEMICOLON));
            }
        }
        if (at(MAP)) {
            advance();
            handleUselessRename();
        }
        else if (at(AS_KEYWORD)) {
            advance();
            expect(IDENTIFIER, "Expecting identifier", TokenSet.create(SEMICOLON));
        }
        cosumeIf(SEMICOLON);
        importDirective.done(IMPORT_DIRECTIVE);
    }

    private void cosumeIf(JetToken token) {
        if (at(token)) advance();
    }

    private void handleUselessRename() {
        if (at(AS_KEYWORD)) {
            PsiBuilder.Marker as = mark();
            advance();
            cosumeIf(IDENTIFIER);
            as.error("Cannot rename a all imported items to one identifier");
        }
    }

    private void parseTopLevelObject() {
        PsiBuilder.Marker decl = mark();
        if (!parseModifierList()) {
            decl.drop();
            advance();
            return;
        }

        JetToken keywordToken = tt();
        JetNodeType declType = null;
        if (keywordToken == CLASS_KEYWORD) {
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
        else if (keywordToken == NAMESPACE_KEYWORD) {
            declType = parseBlockNamespace();
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

    private JetNodeType parseTypedef() {
        advance(); // TODO
        return TYPEDEF;
    }

    private JetNodeType parseBlockNamespace() {
        advance(); // TODO
        return NAMESPACE;
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

    private JetNodeType parseClass() {
        assert at(CLASS_KEYWORD);
        advance();

        expect(IDENTIFIER, "Class name expected", CLASS_NAME_RECOVERY_SET);
        parseTypeParameterList();

        cosumeIf(WRAPS_KEYWORD);
        if (at(LPAR)) {
            parsePrimaryConstructorParameterList();
        }

        if (at(COLON)) {
            advance();
            parseDelegationSpecifierList();
        }

        if (at(LBRACE)) {
            parseClassBody();
        }

        return CLASS;
    }

    private void parseClassBody() {
        assert at(LBRACE);
        PsiBuilder.Marker body = mark();
        advance();

        while (!eof()) {
            if (at(RBRACE)) {
                break;
            }
            advance();
        }
        expect(RBRACE, "Missing '}");
        body.done(CLASS_BODY);
    }

    private void parseDelegationSpecifierList() {
        PsiBuilder.Marker list = mark();
        while (true) {
            parseDelegationSpecifier();
            if (!at(COMMA)) break;
            advance();
        }

        list.done(DELEGATION_SPECIFIER_LIST);
    }

    private void parseDelegationSpecifier() {
        PsiBuilder.Marker specifier = mark();
        parseAttributeList();

        PsiBuilder.Marker delegator = mark();
        parseTypeRef(); // TODO: Error recovery!!!
        if (at(BY_KEYWORD)) {
            advance();
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
        advance();
        while (true) {
            if (at(IDENTIFIER) && lookahead(1) == EQ) {
                PsiBuilder.Marker named = mark();
                advance();
                advance();
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
        advance();

        while (true) {
            parsePrimaryConstructorParameter();
            if (!at(COMMA)) break;
            advance();
        }

        expect(RPAR, "')' expected");

        cons.done(PRIMARY_CONSTRUCTOR_PARAMETERS_LIST);
    }

    private void parsePrimaryConstructorParameter() {
        PsiBuilder.Marker param = mark();
        parseModifierList();

        if (at(VAR_KEYWORD) || at(VAL_KEYWORD)) {
            advance();
        }

        parseFunctionParameterRest();

        param.done(PRIMARY_CONSTRUCTOR_PARAMETER);
    }

    private void parseFunctionParameterRest() {
        expect(IDENTIFIER, "Parameter name expected", PARAMETER_NAME_RECOVERY_SET);

        if (at(COLON)) {
            advance();
            parseTypeRef();
        }
        else {
            error("Parameters must have type annotation");
        }

        if (at(EQ)) {
            advance();
            parseExpression();
        }
    }

    private void parseExpression() {
        advance();
        // TODO
    }

    private void parseTypeParameterList() {
        PsiBuilder.Marker list = mark();
        if (tt() == LT) {
            advance();

            while (true) {
                parseTypeParameter();

                if (!at(COMMA)) break;
                advance();
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
            advance();
            parseTypeRef();
        }

        mark.done(TYPE_PARAMETER);

    }

    private void parseTypeRef() {
        advance();
        // tODO:
    }

    private boolean parseModifierList() {
        // TODO
        return true;
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
            advance();
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
        advance();
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
}
