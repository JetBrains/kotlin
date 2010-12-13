/*
 * @author max
 */
package org.jetbrains.jet.lang.parsing;

import com.intellij.lang.ITokenTypeRemapper;
import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeType;
import org.jetbrains.jet.lexer.JetSoftKeywordToken;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;

import static org.jetbrains.jet.JetNodeTypes.*;
import static org.jetbrains.jet.lexer.JetTokens.*;

public class JetParsing {
    public static final TokenSet CLASS_NAME_FOLLOW = TokenSet.create(LT, WRAPS_KEYWORD, LPAR, COLON, LBRACE);
    public static final TokenSet TYPE_PARAMETER_GT_FOLLOW = TokenSet.create(WHERE_KEYWORD, WRAPS_KEYWORD, LPAR, COLON, LBRACE, GT);
    public static final TokenSet PARAMETER_NAME_FOLLOW = TokenSet.create(COLON, EQ, COMMA, RPAR);
    private final PsiBuilder myBuilder;

    public JetParsing(PsiBuilder myBuilder) {
        this.myBuilder = myBuilder;
    }


    public void parseFile() {
        PsiBuilder.Marker fileMarker = mark();
        PsiBuilder.Marker namespaceMarker = parsePreamble();

        while (!eof()) {
            parseTopLevelObject();
        }

        if (namespaceMarker != null) {
            namespaceMarker.done(NAMESPACE);
        }

        fileMarker.done(JET_FILE);
    }

    @Nullable
    private PsiBuilder.Marker parsePreamble() {
        if (at(NAMESPACE_KEYWORD)) {
            PsiBuilder.Marker namespaceMarker = mark();
            advance();

            parseQualifiedName();
            return namespaceMarker;
        }

        while (at(IMPORT_KEYWORD)) {
            parseImportDirective();
        }

        return null;
    }

    private void parseImportDirective() {
        // TODO
    }

    private PsiBuilder.Marker mark() {
        return myBuilder.mark();
    }

    private void parseQualifiedName() {
        while (true) {
            if (tt() != IDENTIFIER) {
                if (tt() != DOT) {
                    error("Expecting qualified name");
                    break;
                }
                error("Qualified name must start with an identifier");
                advance();
            }
            else {
                advance();
                if (tt() != DOT) break;
                advance();
            }
        }
    }

    private void error(String message) {
        myBuilder.error(message);
    }

    private void parseTopLevelObject() {
        PsiBuilder.Marker decl = mark();
        if (!parseModifierList()) {
            decl.drop();
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
        return TYPEDEF;
    }

    private JetNodeType parseBlockNamespace() {
        return NAMESPACE;
    }

    private JetNodeType parseProperty() {
        return PROPERTY;
    }

    private JetNodeType parseFunction() {
        return FUN;
    }

    private JetNodeType parseExtension() {
        return EXTENSION;
    }

    private JetNodeType parseClass() {
        assert at(CLASS_KEYWORD);
        advance();

        expect(IDENTIFIER, "Class name expected", CLASS_NAME_FOLLOW);
        parseTypeParameterList();

        if (at(WRAPS_KEYWORD)) advance();
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
        expect(IDENTIFIER, "Parameter name expected", PARAMETER_NAME_FOLLOW);

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

            expect(GT, "Missing '>'", TYPE_PARAMETER_GT_FOLLOW);
        }
        list.done(TYPE_PARAMETER_LIST);
    }

    private void parseTypeParameter() {
        if (TYPE_PARAMETER_GT_FOLLOW.contains(tt())) {
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

    private boolean expect(JetToken expectation, String message) {
        return expect(expectation, message, null);
    }

    private boolean expect(JetToken expectation, String message, TokenSet recoverySet) {
        JetToken tt = tt();
        if (tt == expectation) {
            advance();
            return true;
        }

        if (recoverySet == null || recoverySet.contains(tt)) {
            error(message);
        }
        else {
            errorAndAdvance(message);
        }

        return false;
    }

    private void errorAndAdvance(String message) {
        PsiBuilder.Marker err = mark();
        advance();
        err.error(message);
    }

    private boolean parseModifierList() {
        // TODO
        return true;
    }

    private boolean eof() {
        return myBuilder.eof();
    }

    private void advance() {
        myBuilder.advanceLexer();
    }

    private JetToken tt() {
        return (JetToken) myBuilder.getTokenType();
    }



    private boolean at(final IElementType expectation) {
        JetToken token = tt();
        if (token == expectation) return true;
        if (token == IDENTIFIER && expectation instanceof JetSoftKeywordToken) {
            if (((JetSoftKeywordToken) expectation).getValue().equals(myBuilder.getTokenText())) {
                myBuilder.setTokenTypeRemapper(new ITokenTypeRemapper() {
                    public IElementType filter(IElementType source, int start, int end, CharSequence text) {
                        return expectation;
                    }
                });

                tt();
                myBuilder.setTokenTypeRemapper(null);
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
