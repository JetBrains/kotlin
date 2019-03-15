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
  public _KDocLexer() {
    this((java.io.Reader)null);
  }

  private boolean isLastToken() {
    return zzMarkedPos == zzBuffer.length();
  }

  private boolean yytextContainLineBreaks() {
    return CharArrayUtil.containLineBreaks(zzBuffer, zzStartRead, zzMarkedPos);
  }

  private boolean nextIsNotWhitespace() {
    return zzMarkedPos <= zzBuffer.length() && !Character.isWhitespace(zzBuffer.charAt(zzMarkedPos + 1));
  }

  private boolean prevIsNotWhitespace() {
    return zzMarkedPos != 0 && !Character.isWhitespace(zzBuffer.charAt(zzMarkedPos - 1));
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

WHITE_SPACE_CHAR    =[\ \t\f\n]
NOT_WHITE_SPACE_CHAR=[^\ \t\f\n]

DIGIT=[0-9]
ALPHA=[:jletter:]
TAG_NAME={ALPHA}({ALPHA}|{DIGIT})*
IDENTIFIER={ALPHA}({ALPHA}|{DIGIT}|".")*
QUALIFIED_NAME_START={ALPHA}
QUALIFIED_NAME_CHAR={ALPHA}|{DIGIT}|[\.]
QUALIFIED_NAME={QUALIFIED_NAME_START}{QUALIFIED_NAME_CHAR}*
CODE_LINK=\[{QUALIFIED_NAME}\]
CODE_FENCE_START=("```" | "~~~").*
CODE_FENCE_END=("```" | "~~~")

%%


<YYINITIAL> "/**"                         { yybegin(CONTENTS_BEGINNING);
                                            return KDocTokens.START;            }
"*"+ "/"                                  { if (isLastToken()) return KDocTokens.END;
                                            else return KDocTokens.TEXT; }

<LINE_BEGINNING> "*"+                     { yybegin(CONTENTS_BEGINNING);
                                            return KDocTokens.LEADING_ASTERISK; }

<CONTENTS_BEGINNING> "@"{TAG_NAME} {
    KDocKnownTag tag = KDocKnownTag.Companion.findByTagName(zzBuffer.subSequence(zzStartRead, zzMarkedPos));
    yybegin(tag != null && tag.isReferenceRequired() ? TAG_BEGINNING : TAG_TEXT_BEGINNING);
    return KDocTokens.TAG_NAME;
}

<TAG_BEGINNING> {
    {WHITE_SPACE_CHAR}+ {
        if (yytextContainLineBreaks()) {
            yybegin(LINE_BEGINNING);
        }
        return TokenType.WHITE_SPACE;
    }

    /* Example: @return[x] The return value of function x
                       ^^^
    */
    {CODE_LINK} { yybegin(TAG_TEXT_BEGINNING);
                  return KDocTokens.MARKDOWN_LINK; }

    /* Example: @param aaa The value of aaa
                       ^^^
    */
    {QUALIFIED_NAME} {
        yybegin(TAG_TEXT_BEGINNING);
        return KDocTokens.MARKDOWN_LINK;
    }

    [^\n] {
        yybegin(CONTENTS);
        return KDocTokens.TEXT;
    }
}

<TAG_TEXT_BEGINNING> {
    {WHITE_SPACE_CHAR}+ {
        if (yytextContainLineBreaks()) {
            yybegin(LINE_BEGINNING);
        }
        return TokenType.WHITE_SPACE;
    }

    /* Example: @return[x] The return value of function x
                       ^^^
    */
    {CODE_LINK} { yybegin(CONTENTS);
                  return KDocTokens.MARKDOWN_LINK; }

    [^\n] {
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

    {WHITE_SPACE_CHAR}+ {
        if (yytextContainLineBreaks()) {
            yybegin(LINE_BEGINNING);
            return TokenType.WHITE_SPACE;
        }  else {
            yybegin(yystate() == CONTENTS_BEGINNING ? CONTENTS_BEGINNING : CONTENTS);
            return KDocTokens.TEXT;  // internal white space
        }
    }

    "\\"[\[\]] {
        yybegin(CONTENTS);
        return KDocTokens.MARKDOWN_ESCAPED_CHAR;
    }

    "[" [^\[]* "](" [^)]* ")" {
        yybegin(CONTENTS);
        return KDocTokens.MARKDOWN_INLINE_LINK;
    }

    {CODE_FENCE_START} {
        yybegin(CODE_BLOCK_LINE_BEGINNING);
        return KDocTokens.TEXT;
    }

    /* We're only interested in parsing links that can become code references,
       meaning they contain only identifier characters and characters that can be
       used in type declarations. No brackets, backticks, asterisks or anything like that.
       Also if a link is followed by [ or (, then its destination is a regular HTTP
       link and not a Kotlin identifier, so we don't need to do our parsing and resolution. */
    {CODE_LINK} / [^\(\[] {
        yybegin(CONTENTS);
        return KDocTokens.MARKDOWN_LINK;
    }

    [^\n] {
        yybegin(CONTENTS);
        return KDocTokens.TEXT;
    }
}

<CODE_BLOCK_LINE_BEGINNING> {
    "*"+ {
        yybegin(CODE_BLOCK_CONTENTS_BEGINNING);
        return KDocTokens.LEADING_ASTERISK;
    }
}

<CODE_BLOCK_LINE_BEGINNING, CODE_BLOCK_CONTENTS_BEGINNING> {
    {CODE_FENCE_END} / [ \t\f]* [\n] [ \t\f]* {
        // Code fence end
        yybegin(CONTENTS);
        return KDocTokens.TEXT;
    }
}

<INDENTED_CODE_BLOCK, CODE_BLOCK_LINE_BEGINNING, CODE_BLOCK_CONTENTS_BEGINNING, CODE_BLOCK> {
    {WHITE_SPACE_CHAR}+ {
        if (yytextContainLineBreaks()) {
            yybegin(yystate() == INDENTED_CODE_BLOCK ? LINE_BEGINNING : CODE_BLOCK_LINE_BEGINNING);
            return TokenType.WHITE_SPACE;
        }
        return KDocTokens.CODE_BLOCK_TEXT;
    }

    [^\n] {
        yybegin(yystate() == INDENTED_CODE_BLOCK ? INDENTED_CODE_BLOCK : CODE_BLOCK);
        return KDocTokens.CODE_BLOCK_TEXT;
    }
}

[\s\S] { return TokenType.BAD_CHARACTER; }
