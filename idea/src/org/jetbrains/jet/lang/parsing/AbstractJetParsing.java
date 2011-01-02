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
/*package*/ abstract class AbstractJetParsing {
    protected final SemanticWhitespaceAwarePsiBuilder myBuilder;

    public AbstractJetParsing(SemanticWhitespaceAwarePsiBuilder builder) {
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

    protected boolean expectNoAdvance(JetToken expectation, String message) {
        if (at(expectation)) {
            advance(); // expectation
            return true;
        }

        error(message);

        return false;
    }

    protected void errorWithRecovery(String message, TokenSet recoverySet) {
        IElementType tt = tt();
        if (recoverySet == null || recoverySet.contains(tt)
                || (recoverySet.contains(EOL_OR_SEMICOLON)
                        && (eof() || tt == SEMICOLON || myBuilder.eolInLastWhitespace()))) {
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
            if (eof()) return true;
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
        if (expectation == IDENTIFIER && token instanceof JetKeywordToken) {
            JetKeywordToken keywordToken = (JetKeywordToken) token;
            if (keywordToken.isSoft()) {
                myBuilder.remapCurrentToken(IDENTIFIER);
                return true;
            }
        }
        return false;
    }

    protected boolean atSet(IElementType... tokens) {
        return atSet(TokenSet.create(tokens));
    }

    protected boolean atSet(final TokenSet set) {
        IElementType token = tt();
        if (set.contains(token)) return true;
        if (set.contains(EOL_OR_SEMICOLON)) {
            if (eof()) return true;
            if (token == SEMICOLON) return true;
            if (myBuilder.eolInLastWhitespace()) return true;
        }
        if (token == IDENTIFIER) {
            // TODO : this loop seems to be a bad solution
            for (IElementType type : set.getTypes()) {
                if (type instanceof JetKeywordToken) {
                    JetKeywordToken expectedKeyword = (JetKeywordToken) type;
                    if (expectedKeyword.isSoft() && expectedKeyword.getValue().equals(myBuilder.getTokenText())) {
                        myBuilder.remapCurrentToken(type);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected IElementType lookahead(int k) {
        return myBuilder.lookAhead(k);
    }

    protected void consumeIf(JetToken token) {
        if (at(token)) advance(); // token
    }

    // TODO: Migrate to predicates
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

    protected void errorUntilOffset(String mesage, int offset) {
        PsiBuilder.Marker error = mark();
        while (!eof() && myBuilder.getCurrentOffset() < offset) {
            advance();
        }
        error.error(mesage);
    }

    protected int matchTokenStreamPredicate(TokenStreamPattern pattern) {
        PsiBuilder.Marker currentPosition = mark();
        int openAngleBrackets = 0;
        int openBraces = 0;
        int openParentheses = 0;
        int openBrackets = 0;
        while (!eof()) {
            if (pattern.processToken(
                    myBuilder.getCurrentOffset(),
                    pattern.isTopLevel(openAngleBrackets, openBrackets, openBraces, openParentheses))) {
                break;
            }
            if (at(LPAR)) {
                openParentheses++;
            }
            else if (at(LT)) {
                openAngleBrackets++;
            }
            else if (at(LBRACE)) {
                openBraces++;
            }
            else if (at(LBRACKET)) {
                openBrackets++;
            }
            else if (at(RPAR)) {
                openParentheses--;
            }
            else if (at(GT)) {
                openAngleBrackets--;
            }
            else if (at(RBRACE)) {
                openBraces--;
            }
            else if (at(RBRACKET)) {
                openBrackets--;
            }
            advance(); // skip token
        }
        currentPosition.rollbackTo();
        return pattern.result();
    }

    /*
     * Looks for a the last top-level (not inside any {} [] () <>) '.' occurring before a
     * top-level occurrence of a token from the <code>stopSet</code>
     *
     * Returns -1 if no occurrence is found
     *
     * TODO: Migrate to predictaes
     */
    protected int findLastBefore(TokenSet lookFor, TokenSet stopAt, boolean dontStopRightAfterOccurrence) {
        return matchTokenStreamPredicate(new LastBefore(new AtSet(lookFor), new AtSet(stopAt), dontStopRightAfterOccurrence));
    }

    protected boolean eol() {
        return myBuilder.eolInLastWhitespace() || eof();
    }

    protected abstract JetParsing create(SemanticWhitespaceAwarePsiBuilder builder);

    protected JetParsing createTruncatedBuilder(int eofPosition) {
        return create(new TruncatedSemanticWhitespaceAwarePsiBuilder(myBuilder, eofPosition));
    }

    protected class AtOffset implements TokenStreamPredicate {

        private final int offset;

        public AtOffset(int offset) {
            this.offset = offset;
        }

        @Override
        public boolean matching(boolean topLevel) {
            return myBuilder.getCurrentOffset() == offset;
        }

    }

    protected class At implements TokenStreamPredicate {

        private final IElementType lookFor;
        private final boolean topLevelOnly;

        public At(IElementType lookFor, boolean topLevelOnly) {
            this.lookFor = lookFor;
            this.topLevelOnly = topLevelOnly;
        }

        public At(IElementType lookFor) {
            this(lookFor, true);
        }

        @Override
        public boolean matching(boolean topLevel) {
            return (topLevel || !topLevelOnly) && at(lookFor);
        }

    }

    protected class AtSet implements TokenStreamPredicate {
        private final TokenSet lookFor;
        private final boolean topLevelOnly;

        public AtSet(TokenSet lookFor, boolean topLevelOnly) {
            this.lookFor = lookFor;
            this.topLevelOnly = topLevelOnly;
        }

        public AtSet(TokenSet lookFor) {
            this(lookFor, true);
        }

        public AtSet(IElementType... lookFor) {
            this(TokenSet.create(lookFor), true);
        }

        @Override
        public boolean matching(boolean topLevel) {
            return (topLevel || !topLevelOnly) && atSet(lookFor);
        }
    }


}
