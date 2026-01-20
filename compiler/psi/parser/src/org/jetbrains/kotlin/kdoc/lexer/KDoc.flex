package org.jetbrains.kotlin.kdoc.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
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

  /**
    * Stores the symbol that started the current code block (tilda or backtick).
    */
  private char codeFenceChar = '\0';

  /**
    * Counts the length of the [codeFenceChar] string that started the current code block.
    */
  private int codeFenceLength = -1;

  private BlockType lastBlockType;

  private enum BlockType {
      Paragraph,
      Code,
  }

  public _KDocLexer() {
    this((java.io.Reader)null);
  }

  /**
   * It should be used by default instead of `yybegin` except in cases where manual handling of `consecutiveLineBreakCount` is needed.
   * For instance, although the leading asterisk switches to the ` CONTENTS_BEGINNING ` state, it shouldn't affect `consecutiveLineBreakCount`
   * because it's a special char that's invisible from the point of view of the plain Markdown parser.
   */
  private void yybeginAndUpdate(int newState) {
    consecutiveLineBreakCount = newState == LINE_BEGINNING || newState == CODE_BLOCK_LINE_BEGINNING ? consecutiveLineBreakCount + 1 : 0;
    yybegin(newState);
  }

  private boolean isLastToken() {
    return zzMarkedPos == zzBuffer.length();
  }

  private int countRepeating(char c) {
      int current = zzStartRead;
      while (zzBuffer.charAt(current) == c && current < zzMarkedPos) {
          ++current;
      }
      return current - zzStartRead;
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
%state CODE_SPAN_CONTENTS
%state CODE_SPAN_LINE_BEGINNING

WHITE_SPACE_CHAR    = [\ \t\f]
LINE_BREAK_CHAR     = [\r\n]

ESCAPED_CHARS = "\\"[!#$%&'()*+,-./:;<=>?@_`{|}~\"\^\[\\\]]

DIGIT=[0-9]
LETTER = [:jletter:]
PLAIN_IDENTIFIER = {LETTER} ({LETTER} | {DIGIT})*
IDENTIFIER = {PLAIN_IDENTIFIER} | `[^`\n]+`
QUALIFIED_NAME = {IDENTIFIER} ([\.] {IDENTIFIER}?)* // Handle incorrect/incomplete qualifiers for correct resolving
CODE_LINK=\[{QUALIFIED_NAME}\]
BACKTICK_STRING="`"+
TILDA_STRING="~"+
// Fenced code blocks only start at the beginning of a new line and don't contain any backtick
// characters after the initial fence.
// `org.jetbrains.kotlin.kdoc.lexer.KDocLexer` relies on these two types of ending fences.
// If this set is changed, please, update `KDocLexer` accordingly
BACKTICK_CODE_FENCE_START = "``" {BACKTICK_STRING} [^`{LINE_BREAK_CHAR}]*
TILDA_CODE_FENCE_START = "~~" {TILDA_STRING} [^{LINE_BREAK_CHAR}]*
CODE_FENCE_START={BACKTICK_CODE_FENCE_START} | {TILDA_CODE_FENCE_START}
CODE_FENCE_END={BACKTICK_STRING} | {TILDA_STRING}

%%


<YYINITIAL> "/**" {
              yybeginAndUpdate(CONTENTS_BEGINNING);
              return KDocTokens.START;
}
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
              lastBlockType = BlockType.Paragraph;
              KDocKnownTag tag = KDocKnownTag.Companion.findByTagName(zzBuffer.subSequence(zzStartRead, zzMarkedPos));
              yybeginAndUpdate(tag != null && tag.isReferenceRequired() ? TAG_BEGINNING : TAG_TEXT_BEGINNING);
              return KDocTokens.TAG_NAME;
}

<TAG_BEGINNING> {
    {LINE_BREAK_CHAR} {
              yybeginAndUpdate(LINE_BEGINNING);
              return TokenType.WHITE_SPACE;
    }

    {WHITE_SPACE_CHAR}+ {
              return TokenType.WHITE_SPACE;
    }

    /* Example: @return[x] The return value of function x
                       ^^^
    */
    {CODE_LINK} {
              yybeginAndUpdate(TAG_TEXT_BEGINNING);
              return KDocTokens.MARKDOWN_LINK;
    }

    /* Example: @param aaa The value of aaa
                       ^^^
    */
    {QUALIFIED_NAME} {
              yybeginAndUpdate(TAG_TEXT_BEGINNING);
              return KDocTokens.MARKDOWN_LINK;
    }

    [^] {
              yybeginAndUpdate(CONTENTS);
              return KDocTokens.TEXT;
    }
}

<TAG_TEXT_BEGINNING> {
    {LINE_BREAK_CHAR} {
              yybeginAndUpdate(LINE_BEGINNING);
              return TokenType.WHITE_SPACE;
    }

    {WHITE_SPACE_CHAR}+ {
              return TokenType.WHITE_SPACE;
    }

    /* Example: @return[x] The return value of function x
                       ^^^
    */
    {CODE_LINK} {
              yybeginAndUpdate(CONTENTS);
              return KDocTokens.MARKDOWN_LINK;
    }

    [^] {
              yybeginAndUpdate(CONTENTS);
              return KDocTokens.TEXT;
    }
}

<CONTENTS_BEGINNING> {
    {CODE_FENCE_START} / {LINE_BREAK_CHAR} {
              lastBlockType = BlockType.Code;
              codeFenceChar = zzBuffer.charAt(zzStartRead);
              codeFenceLength = countRepeating(codeFenceChar);
              yybeginAndUpdate(CODE_BLOCK_LINE_BEGINNING);
              return KDocTokens.TEXT;
    }
}

<CONTENTS_BEGINNING, CONTENTS> {
    {BACKTICK_STRING} {
              codeFenceChar = zzBuffer.charAt(zzStartRead);
              codeFenceLength = countRepeating(codeFenceChar);
              yybeginAndUpdate(CODE_SPAN_CONTENTS);
              return KDocTokens.TEXT;
      }
}

<CODE_SPAN_CONTENTS> {
    {LINE_BREAK_CHAR} {
              yybeginAndUpdate(CODE_SPAN_LINE_BEGINNING);
              return TokenType.WHITE_SPACE;
      }

    {BACKTICK_STRING} {
              char ch = zzBuffer.charAt(zzStartRead);
              int length = countRepeating(ch);
              if (length == codeFenceLength && ch == codeFenceChar) {
                  // Code span end
                  codeFenceLength = -1;
                  codeFenceChar = '\0';
                  yybeginAndUpdate(CONTENTS);
                  return KDocTokens.TEXT;
              } else {
                  return KDocTokens.CODE_SPAN_TEXT;
              }
      }

    [^] {
              return KDocTokens.CODE_SPAN_TEXT;
      }
}

<CODE_SPAN_LINE_BEGINNING> {
    {WHITE_SPACE_CHAR}+ {
              return TokenType.WHITE_SPACE;
    }

    "*"+ {
              yybeginAndUpdate(CODE_SPAN_CONTENTS);
              return KDocTokens.LEADING_ASTERISK;
    }

}

<LINE_BEGINNING, CONTENTS_BEGINNING, CONTENTS> {
    {LINE_BREAK_CHAR} {
              yybeginAndUpdate(LINE_BEGINNING);
              return TokenType.WHITE_SPACE;
    }

    {WHITE_SPACE_CHAR}+ {
              int state = yystate();

              if (
                  // Recognize indented code blocks if only the line starts with an asterisk(s)
                  state == CONTENTS_BEGINNING &&

                  // If there are more than 4 spaces at the beginning of the line or a tab char, we are trying to recognize indented code block
                  (zzMarkedPos - zzStartRead >= 4 || zzBuffer.charAt(zzStartRead) == '\t' || zzBuffer.charAt(zzMarkedPos - 1) == '\t') &&

                  // If the last block type is paragraph, more than 1 consecutive line break is required
                  (lastBlockType != BlockType.Paragraph || consecutiveLineBreakCount >= 2)
              ) {
                  yybegin(INDENTED_CODE_BLOCK);
                  lastBlockType = BlockType.Code;
                  return KDocTokens.CODE_BLOCK_TEXT;
              }

              yybegin(yystate() == CONTENTS_BEGINNING ? CONTENTS_BEGINNING : CONTENTS);

              return KDocTokens.TEXT;  // internal white space
    }

    {ESCAPED_CHARS} {
              lastBlockType = BlockType.Paragraph;
              yybeginAndUpdate(CONTENTS);
              return KDocTokens.MARKDOWN_ESCAPED_CHAR;
    }

    "(" {
              lastBlockType = BlockType.Paragraph;
              yybeginAndUpdate(CONTENTS);
              return KDocTokens.KDOC_LPAR;
    }

    ")" {
              lastBlockType = BlockType.Paragraph;
              yybeginAndUpdate(CONTENTS);
              return KDocTokens.KDOC_RPAR;
    }

    /* We're only interested in parsing links that can become code references,
       meaning they contain only identifier characters and characters that can be
       used in type declarations. No brackets, backticks, asterisks or anything like that.
       Also if a link is followed by [ or (, then its destination is a regular HTTP
       link and not a Kotlin identifier, so we don't need to do our parsing and resolution. */
    {CODE_LINK} / [^\(\[] {
              lastBlockType = BlockType.Paragraph;
              yybeginAndUpdate(CONTENTS);
              return KDocTokens.MARKDOWN_LINK;
    }

    [^] {
              lastBlockType = BlockType.Paragraph;
              yybeginAndUpdate(CONTENTS);
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
              char ch =  zzBuffer.charAt(zzStartRead);
              int length = countRepeating(ch);
              if (length == codeFenceLength && ch == codeFenceChar) {
                  // Code fence end
                  codeFenceChar = '\0';
                  codeFenceLength = -1;
                  yybeginAndUpdate(CONTENTS);
                  return KDocTokens.TEXT;
              } else {
                  return KDocTokens.CODE_BLOCK_TEXT;
              }
    }
}

<INDENTED_CODE_BLOCK, CODE_BLOCK_LINE_BEGINNING, CODE_BLOCK_CONTENTS_BEGINNING, CODE_BLOCK> {
    {LINE_BREAK_CHAR} {
              yybeginAndUpdate(yystate() == INDENTED_CODE_BLOCK ? LINE_BEGINNING : CODE_BLOCK_LINE_BEGINNING);
              return TokenType.WHITE_SPACE;
    }

    {WHITE_SPACE_CHAR}+ {
              return KDocTokens.CODE_BLOCK_TEXT;
    }

    [^] {
              yybeginAndUpdate(yystate() == INDENTED_CODE_BLOCK ? INDENTED_CODE_BLOCK : CODE_BLOCK);
              return KDocTokens.CODE_BLOCK_TEXT;
    }
}

[\s\S] {
              lastBlockType = BlockType.Paragraph;
              return TokenType.BAD_CHARACTER;
}