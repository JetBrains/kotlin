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
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.lexer.KtKeywordToken;
import org.jetbrains.kotlin.lexer.KtToken;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.utils.strings.StringsKt;

import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.kotlin.lexer.KtTokens.*;

/*package*/ abstract class AbstractKotlinParsing {
    private static final Map<String, KtKeywordToken> SOFT_KEYWORD_TEXTS = new HashMap<>();

    static {
        for (IElementType type : KtTokens.SOFT_KEYWORDS.getTypes()) {
            KtKeywordToken keywordToken = (KtKeywordToken) type;
            assert keywordToken.isSoft();
            SOFT_KEYWORD_TEXTS.put(keywordToken.getValue(), keywordToken);
        }
    }

    static {
        for (IElementType token : KtTokens.KEYWORDS.getTypes()) {
            assert token instanceof KtKeywordToken : "Must be KtKeywordToken: " + token;
            assert !((KtKeywordToken) token).isSoft() : "Must not be soft: " + token;
        }
    }

    protected final SemanticWhitespaceAwarePsiBuilder myBuilder;
    protected final boolean isLazy;

    public AbstractKotlinParsing(SemanticWhitespaceAwarePsiBuilder builder) {
        this(builder, true);
    }

    public AbstractKotlinParsing(SemanticWhitespaceAwarePsiBuilder builder, boolean isLazy) {
        this.myBuilder = builder;
        this.isLazy = isLazy;
    }

    protected IElementType getLastToken() {
        int i = 1;
        int currentOffset = myBuilder.getCurrentOffset();
        while (i <= currentOffset && WHITE_SPACE_OR_COMMENT_BIT_SET.contains(myBuilder.rawLookup(-i))) {
            i++;
        }
        return myBuilder.rawLookup(-i);
    }

    protected boolean expect(KtToken expectation, String message) {
        return expect(expectation, message, null);
    }

    protected PsiBuilder.Marker mark() {
        return myBuilder.mark();
    }

    protected void error(String message) {
        myBuilder.error(message);
    }

    protected boolean expect(KtToken expectation, String message, TokenSet recoverySet) {
        if (expect(expectation)) {
            return true;
        }

        errorWithRecovery(message, recoverySet);

        return false;
    }

    protected boolean expect(KtToken expectation) {
        if (at(expectation)) {
            advance(); // expectation
            return true;
        }

        if (expectation == KtTokens.IDENTIFIER && "`".equals(myBuilder.getTokenText())) {
            advance();
        }

        return false;
    }

    protected void expectNoAdvance(KtToken expectation, String message) {
        if (at(expectation)) {
            advance(); // expectation
            return;
        }

        error(message);
    }

    protected void errorWithRecovery(String message, TokenSet recoverySet) {
        IElementType tt = tt();
        if (recoverySet == null ||
            recoverySet.contains(tt) ||
            tt == LBRACE || tt == RBRACE ||
            (recoverySet.contains(EOL_OR_SEMICOLON) && (eof() || tt == SEMICOLON || myBuilder.newlineBeforeCurrentToken()))) {
            error(message);
        }
        else {
            errorAndAdvance(message);
        }
    }

    protected void errorAndAdvance(String message) {
        errorAndAdvance(message, 1);
    }

    protected void errorAndAdvance(String message, int advanceTokenCount) {
        PsiBuilder.Marker err = mark();
        advance(advanceTokenCount);
        err.error(message);
    }

    protected boolean eof() {
        return myBuilder.eof();
    }

    protected void advance() {
        // TODO: how to report errors on bad characters? (Other than highlighting)
        myBuilder.advanceLexer();
    }

    protected void advance(int advanceTokenCount) {
        for (int i = 0; i < advanceTokenCount; i++) {
            advance(); // erroneous token
        }
    }

    protected void advanceAt(IElementType current) {
        assert _at(current);
        myBuilder.advanceLexer();
    }

    protected int getTokenId() {
        IElementType elementType = tt();
        return (elementType instanceof KtToken) ? ((KtToken)elementType).tokenId : INVALID_Id;
    }

    protected IElementType tt() {
        return myBuilder.getTokenType();
    }

    /**
     * Side-effect-free version of at()
     */
    protected boolean _at(IElementType expectation) {
        IElementType token = tt();
        return tokenMatches(token, expectation);
    }

    private boolean tokenMatches(IElementType token, IElementType expectation) {
        if (token == expectation) return true;
        if (expectation == EOL_OR_SEMICOLON) {
            if (eof()) return true;
            if (token == SEMICOLON) return true;
            if (myBuilder.newlineBeforeCurrentToken()) return true;
        }
        return false;
    }

    protected boolean at(IElementType expectation) {
        if (_at(expectation)) return true;
        IElementType token = tt();
        if (token == IDENTIFIER && expectation instanceof KtKeywordToken) {
            KtKeywordToken expectedKeyword = (KtKeywordToken) expectation;
            if (expectedKeyword.isSoft() && expectedKeyword.getValue().equals(myBuilder.getTokenText())) {
                myBuilder.remapCurrentToken(expectation);
                return true;
            }
        }
        if (expectation == IDENTIFIER && token instanceof KtKeywordToken) {
            KtKeywordToken keywordToken = (KtKeywordToken) token;
            if (keywordToken.isSoft()) {
                myBuilder.remapCurrentToken(IDENTIFIER);
                return true;
            }
        }
        return false;
    }

    /**
     * Side-effect-free version of atSet()
     */
    protected boolean _atSet(TokenSet set) {
        IElementType token = tt();
        if (set.contains(token)) return true;
        if (set.contains(EOL_OR_SEMICOLON)) {
            if (eof()) return true;
            if (token == SEMICOLON) return true;
            if (myBuilder.newlineBeforeCurrentToken()) return true;
        }
        return false;
    }

    protected boolean atSet(TokenSet set) {
        if (_atSet(set)) return true;
        IElementType token = tt();
        if (token == IDENTIFIER) {
            KtKeywordToken keywordToken = SOFT_KEYWORD_TEXTS.get(myBuilder.getTokenText());
            if (keywordToken != null && set.contains(keywordToken)) {
                myBuilder.remapCurrentToken(keywordToken);
                return true;
            }
        }
        else {
            // We know at this point that <code>set</code> does not contain <code>token</code>
            if (set.contains(IDENTIFIER) && token instanceof KtKeywordToken) {
                if (((KtKeywordToken) token).isSoft()) {
                    myBuilder.remapCurrentToken(IDENTIFIER);
                    return true;
                }
            }
        }
        return false;
    }

    protected IElementType lookahead(int k) {
        return myBuilder.lookAhead(k);
    }

    protected boolean consumeIf(KtToken token) {
        if (at(token)) {
            advance(); // token
            return true;
        }
        return false;
    }

    // TODO: Migrate to predicates
    protected void skipUntil(TokenSet tokenSet) {
        boolean stopAtEolOrSemi = tokenSet.contains(EOL_OR_SEMICOLON);
        while (!eof() && !tokenSet.contains(tt()) && !(stopAtEolOrSemi && at(EOL_OR_SEMICOLON))) {
            advance();
        }
    }

    protected void errorUntil(String message, TokenSet tokenSet) {
        assert tokenSet.contains(LBRACE) : "Cannot include LBRACE into error element!";
        assert tokenSet.contains(RBRACE) : "Cannot include RBRACE into error element!";
        PsiBuilder.Marker error = mark();
        skipUntil(tokenSet);
        error.error(message);
    }

    protected static void errorIf(PsiBuilder.Marker marker, boolean condition, String message) {
        if (condition) {
            marker.error(message);
        }
        else {
            marker.drop();
        }
    }

    protected class OptionalMarker {
        private final PsiBuilder.Marker marker;
        private final int offset;

        public OptionalMarker(boolean actuallyMark) {
            marker = actuallyMark ? mark() : null;
            offset = myBuilder.getCurrentOffset();
        }

        public void done(IElementType elementType) {
            if (marker == null) return;
            marker.done(elementType);
        }

        public void error(String message) {
            if (marker == null) return;
            if (offset == myBuilder.getCurrentOffset()) {
                marker.drop(); // no empty errors
            }
            else {
                marker.error(message);
            }
        }

        public void drop() {
            if (marker == null) return;
            marker.drop();
        }
    }

    protected int matchTokenStreamPredicate(TokenStreamPattern pattern) {
        PsiBuilder.Marker currentPosition = mark();
        Stack<IElementType> opens = new Stack<>();
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
            switch (getTokenId()) {
                case LPAR_Id:
                    openParentheses++;
                    opens.push(LPAR);
                    break;
                case LT_Id:
                    openAngleBrackets++;
                    opens.push(LT);
                    break;
                case LBRACE_Id:
                    openBraces++;
                    opens.push(LBRACE);
                    break;
                case LBRACKET_Id:
                    openBrackets++;
                    opens.push(LBRACKET);
                    break;
                case RPAR_Id:
                    openParentheses--;
                    if (opens.isEmpty() || opens.pop() != LPAR) {
                        if (pattern.handleUnmatchedClosing(RPAR)) {
                            break;
                        }
                    }
                    break;
                case GT_Id:
                    openAngleBrackets--;
                    break;
                case RBRACE_Id:
                    openBraces--;
                    break;
                case RBRACKET_Id:
                    openBrackets--;
                    break;
            }

            advance(); // skip token
        }

        currentPosition.rollbackTo();

        return pattern.result();
    }

    protected boolean eol() {
        return myBuilder.newlineBeforeCurrentToken() || eof();
    }

    protected static void closeDeclarationWithCommentBinders(@NotNull PsiBuilder.Marker marker, @NotNull IElementType elementType, boolean precedingNonDocComments) {
        marker.done(elementType);
        marker.setCustomEdgeTokenBinders(precedingNonDocComments ? PrecedingCommentsBinder.INSTANCE : PrecedingDocCommentsBinder.INSTANCE,
                                         TrailingCommentsBinder.INSTANCE);
    }

    protected abstract KotlinParsing create(SemanticWhitespaceAwarePsiBuilder builder);

    protected KotlinParsing createTruncatedBuilder(int eofPosition) {
        return create(new TruncatedSemanticWhitespaceAwarePsiBuilder(myBuilder, eofPosition));
    }

    protected class At extends AbstractTokenStreamPredicate {

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

    protected class AtSet extends AbstractTokenStreamPredicate {
        private final TokenSet lookFor;
        private final TokenSet topLevelOnly;

        public AtSet(TokenSet lookFor, TokenSet topLevelOnly) {
            this.lookFor = lookFor;
            this.topLevelOnly = topLevelOnly;
        }

        public AtSet(TokenSet lookFor) {
            this(lookFor, lookFor);
        }

        @Override
        public boolean matching(boolean topLevel) {
            return (topLevel || !atSet(topLevelOnly)) && atSet(lookFor);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @TestOnly
    public String currentContext() {
        return StringsKt.substringWithContext(myBuilder.getOriginalText(), myBuilder.getCurrentOffset(), myBuilder.getCurrentOffset(), 20);
    }
}
