package org.jetbrains.jet.lang.parsing;

import com.intellij.lang.PsiBuilder;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.lexer.JetToken;

import static org.jetbrains.jet.lexer.JetTokens.*;

/**
 * @author abreslav
 */
/*package*/ class AbstractJetParsing {
    protected final SemanticWitespaceAwarePsiBuilder myBuilder;

    public AbstractJetParsing(SemanticWitespaceAwarePsiBuilder builder) {
        this.myBuilder = builder;
    }

    protected boolean expect(JetToken expectation, String message) {
        return expect(expectation, message, null);
    }

    protected PsiBuilder.Marker mark() {
        return myBuilder.mark();
    }

    protected void error(String message) {
        myBuilder.error(message);
    }

    protected boolean expect(JetToken expectation, String message, TokenSet recoverySet) {
        if (at(expectation)) {
            advance(); // expectation
            return true;
        }

        errorWithRecovery(message, recoverySet);

        return false;
    }

    protected void errorWithRecovery(String message, TokenSet recoverySet) {
        IElementType tt = tt();
        if (recoverySet == null || recoverySet.contains(tt)
                || (recoverySet.contains(EOL_OR_SEMICOLON)
                        && (tt == SEMICOLON || myBuilder.eolInLastWhitespace()))) {
            error(message);
        }
        else {
            errorAndAdvance(message);
        }
    }

    protected boolean errorAndAdvance(String message) {
        PsiBuilder.Marker err = mark();
        advance(); // erroneous token
        err.error(message);
        return false;
    }

    protected boolean eof() {
        return myBuilder.eof();
    }

    protected void advance() {
        myBuilder.advanceLexer();
    }

    protected IElementType tt() {
        IElementType tokenType = myBuilder.getTokenType();
        // TODO: review
        if (tokenType == TokenType.BAD_CHARACTER) errorAndAdvance("Bad character");
        return tokenType;
    }

    protected boolean at(final IElementType expectation) {
        IElementType token = tt();
        if (token == expectation) return true;
        if (expectation == EOL_OR_SEMICOLON) {
            if (token == SEMICOLON) return true;
            if (myBuilder.eolInLastWhitespace()) return true;
        }
        if (token == IDENTIFIER && expectation instanceof JetKeywordToken) {
            JetKeywordToken expectedKeyword = (JetKeywordToken) expectation;
            if (expectedKeyword.isSoft() && expectedKeyword.getValue().equals(myBuilder.getTokenText())) {
                myBuilder.remapCurrentToken(expectation);
                return true;
            }
        }
        return false;
    }

    protected IElementType lookahead(int k) {
        PsiBuilder.Marker tmp = mark();
        for (int i = 0; i < k; i++) advance();

        IElementType tt = tt();
        tmp.rollbackTo();
        return tt;
    }

    protected void consumeIf(JetToken token) {
        if (at(token)) advance(); // token
    }

    protected void skipUntil(TokenSet tokenSet) {
        boolean stopAtEolOrSemi = tokenSet.contains(EOL_OR_SEMICOLON);
        while (!eof() && !tokenSet.contains(tt()) && !(stopAtEolOrSemi && at(EOL_OR_SEMICOLON))) {
            advance();
        }
    }

    protected void errorUntil(String mesage, TokenSet tokenSet) {
        PsiBuilder.Marker error = mark();
        skipUntil(tokenSet);
        error.error(mesage);
    }

}
