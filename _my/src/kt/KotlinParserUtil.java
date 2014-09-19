package kt;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.lang.WhitespacesAndCommentsBinder;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.lexer.JetTokens;

import static org.jetbrains.jet.lexer.JetTokens.*;

/**
 * Created by user on 8/18/14.
 */
public class KotlinParserUtil extends GeneratedParserUtilBase {


    public interface SemanticWhitespaceAwarePsiBuilder extends PsiBuilder {
        // TODO: comments go to wrong place when an empty element is created, see IElementType.isLeftBound()

        boolean newlineBeforeCurrentToken();
        void disableNewlines();
        void enableNewlines();
        void restoreNewlinesState();

        void restoreJoiningComplexTokensState();
        void enableJoiningComplexTokens();
        void disableJoiningComplexTokens();
    }


    public static class SemanticWhitespaceAwarePsiBuilderImpl extends Builder implements SemanticWhitespaceAwarePsiBuilder {
        public static final TokenSet complexTokens = TokenSet.create(JetTokens.SAFE_ACCESS, JetTokens.ELVIS, JetTokens.EXCLEXCL);

        private final Stack<Boolean> joinComplexTokens = new Stack<Boolean>();
        private final Stack<Marker> joinComplexTokensMarkers = new Stack<Marker>();

        private final Stack<Boolean> newlinesEnabled = new Stack<Boolean>();
        private final Stack<Marker> newlinesEnabledMarkers = new Stack<Marker>();

        public SemanticWhitespaceAwarePsiBuilderImpl(PsiBuilder delegate, ErrorState state_, PsiParser parser_) {
            super(delegate, state_, parser_);
            Marker marker = new Marker() {
                @Override
                public Marker precede() {
                    return null;
                }

                @Override
                public void drop() {

                }

                @Override
                public void rollbackTo() {

                }

                @Override
                public void done(IElementType type) {

                }

                @Override
                public void collapse(IElementType type) {

                }

                @Override
                public void doneBefore(IElementType type, Marker before) {

                }

                @Override
                public void doneBefore(IElementType type, Marker before, String errorMessage) {

                }

                @Override
                public void error(String message) {

                }

                @Override
                public void errorBefore(String message, Marker before) {

                }

                @Override
                public void setCustomEdgeTokenBinders(@Nullable WhitespacesAndCommentsBinder left, @Nullable WhitespacesAndCommentsBinder right) {

                }
            };
            newlinesEnabled.push(true);
            newlinesEnabledMarkers.push(marker);
            joinComplexTokens.push(true);
            joinComplexTokensMarkers.push(marker);
        }

        @Override
        public boolean newlineBeforeCurrentToken() {
            if (!newlinesEnabled.peek()) return false;

            if (eof()) return true;

            // TODO: maybe, memoize this somehow?
            for (int i = 1; i <= getCurrentOffset(); i++) {
                IElementType previousToken = rawLookup(-i);

                if (previousToken == JetTokens.BLOCK_COMMENT
                        || previousToken == JetTokens.DOC_COMMENT
                        || previousToken == JetTokens.EOL_COMMENT
                        || previousToken == JetTokens.SHEBANG_COMMENT) {
                    continue;
                }

                if (previousToken != JetTokens.WHITE_SPACE) {
                    break;
                }

                int previousTokenStart = rawTokenTypeStart(-i);
                int previousTokenEnd = rawTokenTypeStart(-i + 1);

                assert previousTokenStart >= 0;
                assert previousTokenEnd < getOriginalText().length();

                for (int j = previousTokenStart; j < previousTokenEnd; j++) {
                    if (getOriginalText().charAt(j) == '\n') {
                        return true;
                    }
                }
            }

            return false;
        }

        @Override
        public void disableNewlines() {
            newlinesEnabled.push(false);
        }

        @Override
        public void enableNewlines() {
            newlinesEnabled.push(true);
        }

        @Override
        public void restoreNewlinesState() {
            assert newlinesEnabled.size() > 1;
            newlinesEnabled.pop();
        }

        private boolean joinComplexTokens() {
            return joinComplexTokens.peek();
        }

        @Override
        public void restoreJoiningComplexTokensState() {
            joinComplexTokens.pop();
        }

        @Override
        public void enableJoiningComplexTokens() {
            joinComplexTokens.push(true);
        }

        @Override
        public void disableJoiningComplexTokens() {
            joinComplexTokens.push(false);
        }

        @Override
        public IElementType getTokenType() {
            if (!joinComplexTokens()) return super.getTokenType();
            return getJoinedTokenType(super.getTokenType(), 1);
        }

        private IElementType getJoinedTokenType(IElementType rawTokenType, int rawLookupSteps) {
            if (rawTokenType == JetTokens.QUEST) {
                IElementType nextRawToken = rawLookup(rawLookupSteps);
                if (nextRawToken == JetTokens.DOT) return JetTokens.SAFE_ACCESS;
                if (nextRawToken == JetTokens.COLON) return JetTokens.ELVIS;
            }
            else if (rawTokenType == JetTokens.EXCL) {
                IElementType nextRawToken = rawLookup(rawLookupSteps);
                if (nextRawToken == JetTokens.EXCL) return JetTokens.EXCLEXCL;
            }
            return rawTokenType;
        }

        @Override
        public void advanceLexer() {
            if (!joinComplexTokens()) {
                super.advanceLexer();
                return;
            }
            IElementType tokenType = getTokenType();
            if (complexTokens.contains(tokenType)) {
                Marker mark = mark();
                super.advanceLexer();
                super.advanceLexer();
                mark.collapse(tokenType);
            }
            else {
                super.advanceLexer();
            }
        }

        @Override
        public String getTokenText() {
            if (!joinComplexTokens()) return super.getTokenText();
            IElementType tokenType = getTokenType();
            if (complexTokens.contains(tokenType)) {
                if (tokenType == JetTokens.ELVIS) return "?:";
                if (tokenType == JetTokens.SAFE_ACCESS) return "?.";
            }
            return super.getTokenText();
        }

        @Override
        public IElementType lookAhead(int steps) {
            if (!joinComplexTokens()) return super.lookAhead(steps);

            if (complexTokens.contains(getTokenType())) {
                return super.lookAhead(steps + 1);
            }
            return getJoinedTokenType(super.lookAhead(steps), 2);
        }
    }





    public static PsiBuilder adapt_builder_(IElementType root, PsiBuilder builder, PsiParser parser) {
        //builder.setDebugMode(true);
        return adapt_builder_(root, builder, parser, null);
    }

    public static PsiBuilder adapt_builder_(IElementType root, PsiBuilder builder, PsiParser parser, TokenSet[] extendsSets) {
        ErrorState state = new ErrorState();
        ErrorState.initState(state, builder, root, extendsSets);
        //builder.setDebugMode(true);
        return new SemanticWhitespaceAwarePsiBuilderImpl(builder, state, parser);
    }

    public static boolean newlineBeforeCurrentToken(PsiBuilder builder_, int level_) {
        return ((SemanticWhitespaceAwarePsiBuilderImpl)builder_).newlineBeforeCurrentToken();
    }


    public static final TokenSet ALLOW_NEWLINE_OPERATIONS = TokenSet.create(
            JetTokens.DOT, JetTokens.SAFE_ACCESS,
            JetTokens.COLON, JetTokens.AS_KEYWORD, JetTokens.AS_SAFE,
            JetTokens.ELVIS,
            // Can't allow `is` and `!is` because of when entry conditions: IS_KEYWORD, NOT_IS,
            JetTokens.ANDAND,
            JetTokens.OROR
    );

    public static boolean interruptedWithNewLine(PsiBuilder builder_, int level_) {
        return !ALLOW_NEWLINE_OPERATIONS.contains(builder_.getTokenType()) &&
               ((SemanticWhitespaceAwarePsiBuilderImpl)builder_).newlineBeforeCurrentToken();
    }

    public static boolean enableNewlines(PsiBuilder builder_, int level_, PsiBuilder.Marker marker) {
        SemanticWhitespaceAwarePsiBuilderImpl builder = (SemanticWhitespaceAwarePsiBuilderImpl)builder_;
        assert builder.newlinesEnabled.size() == builder.newlinesEnabledMarkers.size();
        builder.enableNewlines();
        builder.newlinesEnabledMarkers.push(marker);
        assert builder.newlinesEnabled.size() == builder.newlinesEnabledMarkers.size();
        return true;
    }

    public static boolean disableNewlines(PsiBuilder builder_, int level_, PsiBuilder.Marker marker) {
        SemanticWhitespaceAwarePsiBuilderImpl builder = (SemanticWhitespaceAwarePsiBuilderImpl)builder_;
        assert builder.newlinesEnabled.size() == builder.newlinesEnabledMarkers.size();
        builder.disableNewlines();
        builder.newlinesEnabledMarkers.push(marker);
        assert builder.newlinesEnabled.size() == builder.newlinesEnabledMarkers.size();
        return true;
    }

    public static boolean restoreNewlinesState(PsiBuilder builder_, int level_) {
        SemanticWhitespaceAwarePsiBuilderImpl builder = (SemanticWhitespaceAwarePsiBuilderImpl)builder_;
        assert builder.newlinesEnabled.size() == builder.newlinesEnabledMarkers.size();
        builder.restoreNewlinesState();
        builder.newlinesEnabledMarkers.pop();
        assert builder.newlinesEnabled.size() == builder.newlinesEnabledMarkers.size();
        return true;
    }

    public static boolean enableJoiningComplexTokens(PsiBuilder builder_, int level_, PsiBuilder.Marker marker) {
        SemanticWhitespaceAwarePsiBuilderImpl builder = (SemanticWhitespaceAwarePsiBuilderImpl)builder_;
        assert builder.joinComplexTokens.size() == builder.joinComplexTokensMarkers.size();
        builder.enableJoiningComplexTokens();
        builder.joinComplexTokensMarkers.push(marker);
        assert builder.joinComplexTokens.size() == builder.joinComplexTokensMarkers.size();
        return true;
    }

    public static boolean disableJoiningComplexTokens(PsiBuilder builder_, int level_, PsiBuilder.Marker marker) {
        SemanticWhitespaceAwarePsiBuilderImpl builder = (SemanticWhitespaceAwarePsiBuilderImpl)builder_;
        assert builder.joinComplexTokens.size() == builder.joinComplexTokensMarkers.size();
        builder.disableJoiningComplexTokens();
        builder.joinComplexTokensMarkers.push(marker);
        assert builder.joinComplexTokens.size() == builder.joinComplexTokensMarkers.size();
        return true;
    }

    public static boolean restoreJoiningComplexTokensState(PsiBuilder builder_, int level_) {
        SemanticWhitespaceAwarePsiBuilderImpl builder = (SemanticWhitespaceAwarePsiBuilderImpl)builder_;
        assert builder.joinComplexTokens.size() == builder.joinComplexTokensMarkers.size();
        builder.restoreJoiningComplexTokensState();
        builder.joinComplexTokensMarkers.pop();
        assert builder.joinComplexTokens.size() == builder.joinComplexTokensMarkers.size();
        return true;
    }

    public static void restoreAll(PsiBuilder builder_, PsiBuilder.Marker marker) {
        SemanticWhitespaceAwarePsiBuilderImpl builder = (SemanticWhitespaceAwarePsiBuilderImpl)builder_;
        assert builder.newlinesEnabled.size() == builder.newlinesEnabledMarkers.size();
        assert builder.joinComplexTokens.size() == builder.joinComplexTokensMarkers.size();
        while (builder.newlinesEnabledMarkers.size() > 1 &&
                builder.newlinesEnabledMarkers.peek() == marker) {
            builder.newlinesEnabledMarkers.pop();
            builder.restoreNewlinesState();
        }
        while (builder.joinComplexTokensMarkers.size() > 1 &&
                builder.joinComplexTokensMarkers.peek() == marker) {
            builder.joinComplexTokensMarkers.pop();
            builder.restoreJoiningComplexTokensState();
        }
    }

    public static void exit_section_(PsiBuilder builder_,
                                     int level,
                                     PsiBuilder.Marker marker,
                                     @Nullable IElementType elementType,
                                     boolean result,
                                     boolean pinned,
                                     @Nullable Parser eatMore) {
        GeneratedParserUtilBase.exit_section_(builder_, level, marker, elementType, result, pinned, eatMore);
        restoreAll(builder_, marker);
    }

    public static void exit_section_(PsiBuilder builder_,
                                     PsiBuilder.Marker marker,
                                     @Nullable IElementType elementType,
                                     boolean result) {
        GeneratedParserUtilBase.exit_section_(builder_, marker, elementType, result);
        restoreAll(builder_, marker);
    }

    public static boolean consumeToken(PsiBuilder builder_, String text) {
        boolean result = GeneratedParserUtilBase.consumeToken(builder_, text);
        SemanticWhitespaceAwarePsiBuilderImpl builder = (SemanticWhitespaceAwarePsiBuilderImpl) builder_;
        return result;
    }

    public static boolean consumeToken(PsiBuilder builder_, IElementType token) {
        boolean result = GeneratedParserUtilBase.consumeToken(builder_, token);
        return result;
    }

    protected static boolean _at(SemanticWhitespaceAwarePsiBuilderImpl myBuilder, IElementType expectation) {
        IElementType token = myBuilder.getTokenType();
        return tokenMatches(myBuilder, token, expectation);
    }

    private static boolean tokenMatches(SemanticWhitespaceAwarePsiBuilderImpl myBuilder, IElementType token, IElementType expectation) {
        if (token == expectation) return true;
        if (expectation == EOL_OR_SEMICOLON) {
            if (myBuilder.eof()) return true;
            if (token == SEMICOLON) return true;
            if (myBuilder.newlineBeforeCurrentToken()) return true;
        }
        return false;
    }

    public static boolean at(PsiBuilder builder_, int level_, IElementType expectation) {
        SemanticWhitespaceAwarePsiBuilderImpl myBuilder = (SemanticWhitespaceAwarePsiBuilderImpl)builder_;
        if (_at(myBuilder, expectation)) return true;
        IElementType token = myBuilder.getTokenType();
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

    public static boolean dotNext(PsiBuilder builder_, int level_) {
        return builder_.getTokenType() == JetTokens.DOT;
    }

    public static boolean typeStartNext(PsiBuilder builder_, int level_) {
        IElementType token = builder_.getTokenType();
        if (token == JetTokens.LPAR) return true;
        if (token == JetTokens.LBRACKET) return true;
        if (token == JetTokens.CAPITALIZED_THIS_KEYWORD) return true;
        if (token == JetTokens.PACKAGE_KEYWORD) return true;
        return identifierNext(builder_, level_);
    }


    public static boolean identifierNext(PsiBuilder builder_, int level_) {
        IElementType token = builder_.getTokenType();
        if (token == JetTokens.IDENTIFIER) return true;
        boolean isSoft = JetTokens.SOFT_KEYWORDS.contains(token);
        if (!isSoft) return false;
        builder_.remapCurrentToken(JetTokens.IDENTIFIER);
        return true;
    }

}
