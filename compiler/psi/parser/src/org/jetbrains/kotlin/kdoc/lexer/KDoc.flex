package org.jetbrains.kotlin.kdoc.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.text.CharArrayUtil;
import java.lang.Character;
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag;

%%

%unicode
%class _KDocLexer
%implements FlexLexer

%{
  /**
   * Counts the number of line breaks after the previous text, typically paragraph.
   * White spaces as well as leading asterisks aren't considered as text, so, they don't reset the counter.
   * It allows implementing markdown spec in a more convenient way.
   * For instance, indented code blocks require two consecutive line breaks after paragraphs.
   */
  private int consecutiveLineBreakCount;

  public _KDocLexer() {
    this((java.io.Reader)null);
  }

  private boolean isLastToken() {
    return zzMarkedPos == zzBuffer.length();
  }
%}

%function advance
%type IElementType
%eof{
  return;
%eof}

%state LINE_BEGINNING
%state CONTENTS_BEGINNING
%state TAG_BEGINNING
%state TAG_TEXT_BEGINNING
%state CONTENTS
%state CODE_BLOCK
%state CODE_BLOCK_LINE_BEGINNING
%state CODE_BLOCK_CONTENTS_BEGINNING
%state INDENTED_CODE_BLOCK

WHITE_SPACE_CHAR    = [\ \t\f]
LINE_BREAK_CHAR     = [\r\n]
WHITE_SPACE_OR_LINE_BREAK_CHAR     = {WHITE_SPACE_CHAR} | {LINE_BREAK_CHAR}
NOT_WHITE_SPACE_OR_LINE_BREAK_CHAR = [^\ \t\f\r\n]

DIGIT=[0-9]
LETTER = [:jletter:]
PLAIN_IDENTIFIER = {LETTER} ({LETTER} | {DIGIT})*
IDENTIFIER = {PLAIN_IDENTIFIER} | `[^`\n]+`
QUALIFIED_NAME = {IDENTIFIER} ([\.] {IDENTIFIER}?)* // Handle incorrect/incomplete qualifiers for correct resolving
CODE_LINK=\[{QUALIFIED_NAME}\]
CODE_FENCE_START=("```" | "~~~").*
CODE_FENCE_END=("```" | "~~~")

%%


<YYINITIAL> "/**"                         { yybegin(CONTENTS_BEGINNING);
                                            return KDocTokens.START;            }
"*"+ "/" {
              consecutiveLineBreakCount = 0;
              if (isLastToken()) return KDocTokens.END;
              else return KDocTokens.TEXT;
}

<LINE_BEGINNING> {
    {WHITE_SPACE_CHAR}+ {
              return TokenType.WHITE_SPACE;
    }
    "*"+ {
              yybegin(CONTENTS_BEGINNING);
              return KDocTokens.LEADING_ASTERISK;
    }
}

<CONTENTS_BEGINNING> "@"{PLAIN_IDENTIFIER} {
    consecutiveLineBreakCount = 0;
    KDocKnownTag tag = KDocKnownTag.Companion.findByTagName(zzBuffer.subSequence(zzStartRead, zzMarkedPos));
    yybegin(tag != null && tag.isReferenceRequired() ? TAG_BEGINNING : TAG_TEXT_BEGINNING);
    return KDocTokens.TAG_NAME;
}

<TAG_BEGINNING> {
    {LINE_BREAK_CHAR} {
        consecutiveLineBreakCount++;
        yybegin(LINE_BEGINNING);
        return TokenType.WHITE_SPACE;
    }

    {WHITE_SPACE_CHAR}+ {
        return TokenType.WHITE_SPACE;
    }

    /* Example: @return[x] The return value of function x
                       ^^^
    */
    {CODE_LINK} { consecutiveLineBreakCount = 0;
                  yybegin(TAG_TEXT_BEGINNING);
                  return KDocTokens.MARKDOWN_LINK; }

    /* Example: @param aaa The value of aaa
                       ^^^
    */
    {QUALIFIED_NAME} {
        consecutiveLineBreakCount = 0;
        yybegin(TAG_TEXT_BEGINNING);
        return KDocTokens.MARKDOWN_LINK;
    }

    {NOT_WHITE_SPACE_OR_LINE_BREAK_CHAR} {
        consecutiveLineBreakCount = 0;
        yybegin(CONTENTS);
        return KDocTokens.TEXT;
    }
}

<TAG_TEXT_BEGINNING> {
    {LINE_BREAK_CHAR} {
        consecutiveLineBreakCount++;
        yybegin(LINE_BEGINNING);
        return TokenType.WHITE_SPACE;
    }

    {WHITE_SPACE_CHAR}+ {
        return TokenType.WHITE_SPACE;
    }

    /* Example: @return[x] The return value of function x
                       ^^^
    */
    {CODE_LINK} { consecutiveLineBreakCount = 0;
                  yybegin(CONTENTS);
                  return KDocTokens.MARKDOWN_LINK; }

    {NOT_WHITE_SPACE_OR_LINE_BREAK_CHAR} {
        consecutiveLineBreakCount = 0;
        yybegin(CONTENTS);
        return KDocTokens.TEXT;
    }
}

<LINE_BEGINNING, CONTENTS_BEGINNING, CONTENTS> {

    ([\ ]{4}[\ ]*)|([\t]+) {
        if(yystate() == CONTENTS_BEGINNING) {
            yybegin(INDENTED_CODE_BLOCK);
            return KDocTokens.CODE_BLOCK_TEXT;
        }
    }

    {LINE_BREAK_CHAR} {
        consecutiveLineBreakCount++;
        yybegin(LINE_BEGINNING);
        return TokenType.WHITE_SPACE;
    }

    {WHITE_SPACE_CHAR}+ {
        if (yystate() != CONTENTS_BEGINNING) {
            yybegin(CONTENTS);
        }
        return KDocTokens.TEXT;  // internal white space
    }

    "\\"[\[\]] {
        consecutiveLineBreakCount = 0;
        yybegin(CONTENTS);
        return KDocTokens.MARKDOWN_ESCAPED_CHAR;
    }

    "(" {
        consecutiveLineBreakCount = 0;
        yybegin(CONTENTS);
        return KDocTokens.KDOC_LPAR;
    }

    ")" {
        consecutiveLineBreakCount = 0;
        yybegin(CONTENTS);
        return KDocTokens.KDOC_RPAR;
    }

    {CODE_FENCE_START} {
        consecutiveLineBreakCount = 0;
        yybegin(CODE_BLOCK_LINE_BEGINNING);
        return KDocTokens.TEXT;
    }

    /* We're only interested in parsing links that can become code references,
       meaning they contain only identifier characters and characters that can be
       used in type declarations. No brackets, backticks, asterisks or anything like that.
       Also if a link is followed by [ or (, then its destination is a regular HTTP
       link and not a Kotlin identifier, so we don't need to do our parsing and resolution. */
    {CODE_LINK} / [^\(\[] {
        consecutiveLineBreakCount = 0;
        yybegin(CONTENTS);
        return KDocTokens.MARKDOWN_LINK;
    }

    {NOT_WHITE_SPACE_OR_LINE_BREAK_CHAR} {
        consecutiveLineBreakCount = 0;
        yybegin(CONTENTS);
        return KDocTokens.TEXT;
    }
}

<CODE_BLOCK_LINE_BEGINNING> {
    {WHITE_SPACE_CHAR}+ {
        return TokenType.WHITE_SPACE;
    }

    "*"+ {
        yybegin(CODE_BLOCK_CONTENTS_BEGINNING);
        return KDocTokens.LEADING_ASTERISK;
    }
}

<CODE_BLOCK_LINE_BEGINNING, CODE_BLOCK_CONTENTS_BEGINNING> {
    {CODE_FENCE_END} / [ \t\f]* [\n] {
        consecutiveLineBreakCount = 0;
        // Code fence end
        yybegin(CONTENTS);
        return KDocTokens.TEXT;
    }
}

<INDENTED_CODE_BLOCK, CODE_BLOCK_LINE_BEGINNING, CODE_BLOCK_CONTENTS_BEGINNING, CODE_BLOCK> {
    {LINE_BREAK_CHAR} {
        consecutiveLineBreakCount++;
        yybegin(yystate() == INDENTED_CODE_BLOCK ? LINE_BEGINNING : CODE_BLOCK_LINE_BEGINNING);
        return TokenType.WHITE_SPACE;
    }

    {WHITE_SPACE_CHAR}+ {
        return KDocTokens.CODE_BLOCK_TEXT;
    }

    {NOT_WHITE_SPACE_OR_LINE_BREAK_CHAR} {
        consecutiveLineBreakCount = 0;
        if (yystate() != INDENTED_CODE_BLOCK) {
            yybegin(CODE_BLOCK);
        }
        return KDocTokens.CODE_BLOCK_TEXT;
    }
}

[\s\S] {
consecutiveLineBreakCount = 0;
return TokenType.BAD_CHARACTER;
}
