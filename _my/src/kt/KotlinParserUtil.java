package kt;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.lang.WhitespacesAndCommentsBinder;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.parsing.AbstractTokenStreamPredicate;
import org.jetbrains.jet.lang.parsing.LastBefore;
import org.jetbrains.jet.lang.parsing.TokenStreamPattern;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

        private final Stack<Integer> stopAt = new Stack<Integer>();
        private final Stack<Marker> stopAtMarkers = new Stack<Marker>();

        private final Stack<Boolean> shortAnnotations = new Stack<Boolean>();
        private final Stack<Marker> shortAnnotationsMarkers = new Stack<Marker>();

        private final ArrayList<Integer> callWithClosureStackSize = new ArrayList<Integer>();
        private final ArrayList<Marker> callWithClosureStackSizeMarkers = new ArrayList<Marker>();

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

            stopAt.push(Integer.MAX_VALUE);
            stopAtMarkers.push(marker);

            shortAnnotations.push(false);
            shortAnnotationsMarkers.push(marker);

            callWithClosureStackSize.add(Integer.MAX_VALUE / 2);
            callWithClosureStackSizeMarkers.add(marker);

        }

        @Override
        public boolean newlineBeforeCurrentToken() {
            if (!newlinesEnabled.peek()) return false;

            if (eof()) return true;

            // TODO: maybe, memorize this somehow?
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
            for (int i = 0; i < callWithClosureStackSize.size(); ++i) {
                int value = callWithClosureStackSize.get(i);
                callWithClosureStackSize.set(i, value + 1);
            }
        }

        @Override
        public void enableNewlines() {
            newlinesEnabled.push(true);
            for (int i = 0; i < callWithClosureStackSize.size(); ++i) {
                int value = callWithClosureStackSize.get(i);
                callWithClosureStackSize.set(i, value + 1);
            }
        }

        @Override
        public void restoreNewlinesState() {
            assert newlinesEnabled.size() > 1;
            newlinesEnabled.pop();
            for (int i = 0; i < callWithClosureStackSize.size(); ++i) {
                int value = callWithClosureStackSize.get(i);
                callWithClosureStackSize.set(i, value - 1);
            }
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

        /*@Override
        public boolean eof() {
            if (super.eof()) return true;
            return getCurrentOffset() >= stopAt.peek();
        }*/
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
        assert builder.stopAt.size() == builder.stopAtMarkers.size();
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
        while (builder.stopAt.size() > 1 && builder.stopAtMarkers.peek() == marker) {
            builder.stopAtMarkers.pop();
            builder.stopAt.pop();
        }
        while (builder.shortAnnotations.size() > 1 && builder.shortAnnotationsMarkers.peek() == marker) {
            builder.shortAnnotationsMarkers.pop();
            builder.shortAnnotations.pop();
        }
        while (builder.callWithClosureStackSize.size() > 1 &&
               builder.callWithClosureStackSizeMarkers.get(builder.callWithClosureStackSize.size() - 1) == marker) {
            builder.callWithClosureStackSize.remove(builder.callWithClosureStackSize.size() - 1);
            builder.callWithClosureStackSizeMarkers.remove(builder.callWithClosureStackSizeMarkers.size() - 1);
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
        SemanticWhitespaceAwarePsiBuilderImpl builder = (SemanticWhitespaceAwarePsiBuilderImpl)builder_;
        boolean result = GeneratedParserUtilBase.consumeToken(builder_, text);
        if (result && builder_.getCurrentOffset() <= builder.stopAt.peek()) return true;
        return false;
    }

    public static boolean consumeToken(PsiBuilder builder_, IElementType token) {
        SemanticWhitespaceAwarePsiBuilderImpl builder = (SemanticWhitespaceAwarePsiBuilderImpl)builder_;
        boolean result = GeneratedParserUtilBase.consumeToken(builder_, token);
        if (result && builder_.getCurrentOffset() <= builder.stopAt.peek()) return true;
        return false;
    }

    public static boolean stopAt(PsiBuilder builder_, int level_, PsiBuilder.Marker marker, int newEOF) {
        SemanticWhitespaceAwarePsiBuilderImpl builder = (SemanticWhitespaceAwarePsiBuilderImpl)builder_;
        builder.stopAt.push(newEOF);
        builder.stopAtMarkers.push(marker);
        return true;
    }

    public static boolean unStop(PsiBuilder builder_, int level_) {
        SemanticWhitespaceAwarePsiBuilderImpl builder = (SemanticWhitespaceAwarePsiBuilderImpl)builder_;
        assert builder.stopAt.size() > 1;
        builder.stopAt.pop();
        builder.stopAtMarkers.pop();
        return true;
    }

    public static boolean allowShortAnnotations(PsiBuilder builder_, int level_, PsiBuilder.Marker marker) {
        SemanticWhitespaceAwarePsiBuilderImpl builder = (SemanticWhitespaceAwarePsiBuilderImpl)builder_;
        builder.shortAnnotations.push(true);
        builder.shortAnnotationsMarkers.push(marker);
        return true;
    }

    public static boolean forbidShortAnnotations(PsiBuilder builder_, int level_, PsiBuilder.Marker marker) {
        SemanticWhitespaceAwarePsiBuilderImpl builder = (SemanticWhitespaceAwarePsiBuilderImpl)builder_;
        builder.shortAnnotations.push(false);
        builder.shortAnnotationsMarkers.push(marker);
        return true;
    }

    public static boolean shortAnnotationsAvailable(PsiBuilder builder_, int level_) {
        SemanticWhitespaceAwarePsiBuilderImpl builder = (SemanticWhitespaceAwarePsiBuilderImpl)builder_;
        return builder.shortAnnotations.peek();
    }

    public static boolean restoreAnnotationsState(PsiBuilder builder_, int level_) {
        SemanticWhitespaceAwarePsiBuilderImpl builder = (SemanticWhitespaceAwarePsiBuilderImpl)builder_;
        assert builder.shortAnnotations.size() > 1;
        builder.shortAnnotations.pop();
        builder.shortAnnotationsMarkers.pop();
        return true;
    }

    protected static class AtSet extends AbstractTokenStreamPredicate {
        private final TokenSet lookFor;
        private final TokenSet topLevelOnly;
        private final SemanticWhitespaceAwarePsiBuilderImpl builder;

        public AtSet(SemanticWhitespaceAwarePsiBuilderImpl builder, TokenSet lookFor, TokenSet topLevelOnly) {
            this.lookFor = lookFor;
            this.topLevelOnly = topLevelOnly;
            this.builder = builder;
        }

        public AtSet(SemanticWhitespaceAwarePsiBuilderImpl builder, TokenSet lookFor) {
            this(builder, lookFor, lookFor);
        }

        public AtSet(SemanticWhitespaceAwarePsiBuilderImpl builder, IElementType... lookFor) {
            this(builder, TokenSet.create(lookFor), TokenSet.create(lookFor));
        }

        @Override
        public boolean matching(boolean topLevel) {
            return (topLevel || !atSet(builder, topLevelOnly)) && atSet(builder, lookFor);
        }
    }

    protected static boolean _atSet(SemanticWhitespaceAwarePsiBuilder builder, TokenSet set) {
        IElementType token = builder.getTokenType();
        if (set.contains(token)) return true;
        if (set.contains(EOL_OR_SEMICOLON)) {
            if (builder.eof()) return true;
            if (token == SEMICOLON) return true;
            if (builder.newlineBeforeCurrentToken()) return true;
        }
        return false;
    }

    private static final Map<String, JetKeywordToken> SOFT_KEYWORD_TEXTS = new HashMap<String, JetKeywordToken>();
    static {
        for (IElementType type : JetTokens.SOFT_KEYWORDS.getTypes()) {
            JetKeywordToken keywordToken = (JetKeywordToken) type;
            assert keywordToken.isSoft();
            SOFT_KEYWORD_TEXTS.put(keywordToken.getValue(), keywordToken);
        }
    }

    protected static boolean atSet(SemanticWhitespaceAwarePsiBuilderImpl builder, TokenSet set) {
        if (_atSet(builder, set)) return true;
        IElementType token = builder.getTokenType();
        if (token == IDENTIFIER) {
            JetKeywordToken keywordToken = SOFT_KEYWORD_TEXTS.get(builder.getTokenText());
            if (keywordToken != null && set.contains(keywordToken)) {
                builder.remapCurrentToken(keywordToken);
                return true;
            }
        }
        else {
            // We know at this point that <code>set</code> does not contain <code>token</code>
            if (set.contains(IDENTIFIER) && token instanceof JetKeywordToken) {
                if (((JetKeywordToken) token).isSoft()) {
                    builder.remapCurrentToken(IDENTIFIER);
                    return true;
                }
            }
        }
        return false;
    }

    protected static int matchTokenStreamPredicate(SemanticWhitespaceAwarePsiBuilderImpl builder, TokenStreamPattern pattern) {
        PsiBuilder.Marker currentPosition = builder.mark();
        Stack<IElementType> opens = new Stack<IElementType>();
        int openAngleBrackets = 0;
        int openBraces = 0;
        int openParentheses = 0;
        int openBrackets = 0;
        while (!builder.eof()) {
            if (pattern.processToken(
                    builder.getCurrentOffset(),
                    pattern.isTopLevel(openAngleBrackets, openBrackets, openBraces, openParentheses))) {
                break;
            }
            if (at(builder, 0, JetTokens.LPAR)) {
                openParentheses++;
                opens.push(LPAR);
            }
            else if (at(builder, 0, JetTokens.LT)) {
                openAngleBrackets++;
                opens.push(LT);
            }
            else if (at(builder, 0, JetTokens.LBRACE)) {
                openBraces++;
                opens.push(LBRACE);
            }
            else if (at(builder, 0, JetTokens.LBRACKET)) {
                openBrackets++;
                opens.push(LBRACKET);
            }
            else if (at(builder, 0, JetTokens.RPAR)) {
                openParentheses--;
                if (opens.isEmpty() || opens.pop() != LPAR) {
                    if (pattern.handleUnmatchedClosing(RPAR)) {
                        break;
                    }
                }
            }
            else if (at(builder, 0, JetTokens.GT)) {
                openAngleBrackets--;
            }
            else if (at(builder, 0, JetTokens.RBRACE)) {
                openBraces--;
            }
            else if (at(builder, 0, JetTokens.RBRACKET)) {
                openBrackets--;
            }
            builder.advanceLexer(); // skip token
        }
        currentPosition.rollbackTo();
        return pattern.result();
    }

    public static boolean stopAtLastDot(final PsiBuilder builder_, final int level_, PsiBuilder.Marker marker) {
        final SemanticWhitespaceAwarePsiBuilderImpl builder = (SemanticWhitespaceAwarePsiBuilderImpl)builder_;
        int lastDot = matchTokenStreamPredicate(builder, new LastBefore(
                new AtSet(builder, DOT, SAFE_ACCESS),
                new AbstractTokenStreamPredicate() {
                    @Override
                    public boolean matching(boolean topLevel) {
                        if (topLevel && (at(builder_, level_, JetTokens.EQ) || at(builder_, level_, JetTokens.COLON))) return true;
                        if (topLevel && at(builder_, level_, JetTokens.IDENTIFIER)) {
                            IElementType lookahead = builder.lookAhead(1);
                            return lookahead != LT && lookahead != DOT && lookahead != SAFE_ACCESS && lookahead != QUEST;
                        }
                        return false;
                    }
                }));
        stopAt(builder_, level_, marker, lastDot);
        return true;
    }

    protected static int findLastBefore(SemanticWhitespaceAwarePsiBuilderImpl builder, TokenSet lookFor, TokenSet stopAt, boolean dontStopRightAfterOccurrence) {
        return matchTokenStreamPredicate(builder, new LastBefore(new AtSet(builder, lookFor), new AtSet(builder, stopAt), dontStopRightAfterOccurrence));
    }

    public static boolean stopInTypeParameter(PsiBuilder builder_, int level_, PsiBuilder.Marker marker) {
        SemanticWhitespaceAwarePsiBuilderImpl builder = (SemanticWhitespaceAwarePsiBuilderImpl)builder_;
        int whenToStop = findLastBefore(builder, TokenSet.create(IDENTIFIER), TokenSet.create(COMMA, GT, COLON), false);
        stopAt(builder_, level_, marker, whenToStop);
        return true;
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

    public static boolean availableCallWithClosure(PsiBuilder builder_, int level_) {
        SemanticWhitespaceAwarePsiBuilderImpl builder = (SemanticWhitespaceAwarePsiBuilderImpl)builder_;
        return builder.callWithClosureStackSize.get(builder.callWithClosureStackSize.size() - 1) > 0;
    }

    public static boolean deleteCallWithClosureCounter(PsiBuilder builder_, int level_) {
        SemanticWhitespaceAwarePsiBuilderImpl builder = (SemanticWhitespaceAwarePsiBuilderImpl)builder_;
        builder.callWithClosureStackSize.remove(builder.callWithClosureStackSize.size() - 1);
        builder.callWithClosureStackSizeMarkers.remove(builder.callWithClosureStackSizeMarkers.size() - 1);
        return true;
    }

    public static boolean addNewCallWithClosureCounter(PsiBuilder builder_, int level_, PsiBuilder.Marker marker) {
        SemanticWhitespaceAwarePsiBuilderImpl builder = (SemanticWhitespaceAwarePsiBuilderImpl)builder_;
        builder.callWithClosureStackSize.add(0);
        builder.callWithClosureStackSizeMarkers.add(marker);
        return true;
    }


}
