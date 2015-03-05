package org.jetbrains.kotlin.kdoc.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.text.CharArrayUtil;
import java.lang.Character;
import org.jetbrains.kotlin.lexer.JetTokens;
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

  private Boolean yytextContainLineBreaks() {
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

WHITE_SPACE_CHAR    =[\ \t\f\n\r]
NOT_WHITE_SPACE_CHAR=[^\ \t\f\n\r]

DIGIT=[0-9]
ALPHA=[:jletter:]
TAG_NAME={ALPHA}({ALPHA}|{DIGIT})*
IDENTIFIER={ALPHA}({ALPHA}|{DIGIT}|".")*
QUALIFIED_NAME_START={ALPHA}
QUALIFIED_NAME_CHAR={ALPHA}|{DIGIT}|[\.]
QUALIFIED_NAME={QUALIFIED_NAME_START}{QUALIFIED_NAME_CHAR}*
CODE_LINK=\[{QUALIFIED_NAME}\]

%%


<YYINITIAL> "/**"                         { yybegin(CONTENTS_BEGINNING);
                                            return KDocTokens.START;            }
"*"+ "/"                                  { if (isLastToken()) return KDocTokens.END;
                                            else return KDocTokens.TEXT; }

<LINE_BEGINNING> "*"+                     { yybegin(CONTENTS_BEGINNING);
                                            return KDocTokens.LEADING_ASTERISK; }

<CONTENTS_BEGINNING> "@"{TAG_NAME} {
    KDocKnownTag tag = KDocKnownTag.findByTagName(zzBuffer.subSequence(zzStartRead, zzMarkedPos));
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

    . {
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

    . {
        yybegin(CONTENTS);
        return KDocTokens.TEXT;
    }
}

<LINE_BEGINNING, CONTENTS_BEGINNING, CONTENTS> {
    {WHITE_SPACE_CHAR}+ {
        if (yytextContainLineBreaks()) {
            yybegin(LINE_BEGINNING);
            return TokenType.WHITE_SPACE;
        } else {
            yybegin(yystate() == CONTENTS_BEGINNING? CONTENTS_BEGINNING:CONTENTS);
            return KDocTokens.TEXT;  // internal white space
        }
    }

    "\\"[\[\]]   { yybegin(CONTENTS);
                   return KDocTokens.MARKDOWN_ESCAPED_CHAR; }

    /* We're only interested in parsing links that can become code references,
       meaning they contain only identifier characters and characters that can be
       used in type declarations. No brackets, backticks, asterisks or anything like that.
       Also if a link is followed by [ or (, then its destination is a regular HTTP
       link and not a Kotlin identifier, so we don't need to do our parsing and resolution. */
    {CODE_LINK} / [^\(\[] { yybegin(CONTENTS);
                  return KDocTokens.MARKDOWN_LINK; }

    .     { yybegin(CONTENTS);
            return KDocTokens.TEXT; }
}

. { return TokenType.BAD_CHARACTER; }
