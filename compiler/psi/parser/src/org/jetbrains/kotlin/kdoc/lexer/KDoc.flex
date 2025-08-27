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
    KDocKnownTag tag = KDocKnownTag.Companion.findByTagName(zzBuffer.subSequence(zzStartRead, zzMarkedPos));
    yybegin(tag != null && tag.isReferenceRequired() ? TAG_BEGINNING : TAG_TEXT_BEGINNING);
    return KDocTokens.TAG_NAME;
}

<TAG_BEGINNING> {
    {LINE_BREAK_CHAR} {
        yybegin(LINE_BEGINNING);
        return TokenType.WHITE_SPACE;
    }

    {WHITE_SPACE_CHAR}+ {
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

    {NOT_WHITE_SPACE_OR_LINE_BREAK_CHAR} {
        yybegin(CONTENTS);
        return KDocTokens.TEXT;
    }
}

<TAG_TEXT_BEGINNING> {
    {LINE_BREAK_CHAR} {
        yybegin(LINE_BEGINNING);
        return TokenType.WHITE_SPACE;
    }

    {WHITE_SPACE_CHAR}+ {
        return TokenType.WHITE_SPACE;
    }

    /* Example: @return[x] The return value of function x
                       ^^^
    */
    {CODE_LINK} { yybegin(CONTENTS);
                  return KDocTokens.MARKDOWN_LINK; }

    {NOT_WHITE_SPACE_OR_LINE_BREAK_CHAR} {
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
        yybegin(LINE_BEGINNING);
        return TokenType.WHITE_SPACE;
    }

    {WHITE_SPACE_CHAR}+ {
        yybegin(yystate() == CONTENTS_BEGINNING ? CONTENTS_BEGINNING : CONTENTS);
        return KDocTokens.TEXT;  // internal white space
    }

    "\\"[\[\]] {
        yybegin(CONTENTS);
        return KDocTokens.MARKDOWN_ESCAPED_CHAR;
    }

    "(" {
        yybegin(CONTENTS);
        return KDocTokens.KDOC_LPAR;
    }

    ")" {
        yybegin(CONTENTS);
        return KDocTokens.KDOC_RPAR;
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

    {NOT_WHITE_SPACE_OR_LINE_BREAK_CHAR} {
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
        // Code fence end
        yybegin(CONTENTS);
        return KDocTokens.TEXT;
    }
}

<INDENTED_CODE_BLOCK, CODE_BLOCK_LINE_BEGINNING, CODE_BLOCK_CONTENTS_BEGINNING, CODE_BLOCK> {
    {LINE_BREAK_CHAR} {
        yybegin(yystate() == INDENTED_CODE_BLOCK ? LINE_BEGINNING : CODE_BLOCK_LINE_BEGINNING);
        return TokenType.WHITE_SPACE;
    }

    {WHITE_SPACE_CHAR}+ {
        return KDocTokens.CODE_BLOCK_TEXT;
    }

    {NOT_WHITE_SPACE_OR_LINE_BREAK_CHAR} {
        yybegin(yystate() == INDENTED_CODE_BLOCK ? INDENTED_CODE_BLOCK : CODE_BLOCK);
        return KDocTokens.CODE_BLOCK_TEXT;
    }
}

[\s\S] { return TokenType.BAD_CHARACTER; }
