package org.jetbrains.kotlin.kdoc.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.text.CharArrayUtil;
import java.lang.Character;

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
%state CONTENTS
%state CODE
%state CODE2

WHITE_SPACE_CHAR    =[\ \t\f\n\r]
NOT_WHITE_SPACE_CHAR=[^\ \t\f\n\r]

DIGIT=[0-9]
ALPHA=[:jletter:]
TAG_NAME={ALPHA}({ALPHA}|{DIGIT})*

MARKDOWN_EMPHASIS=[\*_]

%%


<YYINITIAL> "/**"                         { yybegin(CONTENTS);
                                            return KDocTokens.START;            }
"*"+ "/"                                  { if (isLastToken()) return KDocTokens.END;
                                            else return KDocTokens.TEXT; }

<LINE_BEGINNING> "*"+                     { yybegin(CONTENTS_BEGINNING);
                                            return KDocTokens.LEADING_ASTERISK; }

<CONTENTS_BEGINNING> "@"{TAG_NAME}          { yybegin(CONTENTS);
                                              return KDocTokens.TAG_NAME; }

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

    "[["  { yybegin(CONTENTS);
            return KDocTokens.WIKI_LINK_OPEN; }
    "]]"  { yybegin(CONTENTS);
            return KDocTokens.WIKI_LINK_CLOSE; }

    .     { yybegin(CONTENTS);
            return KDocTokens.TEXT; }
}

. { return TokenType.BAD_CHARACTER; }
