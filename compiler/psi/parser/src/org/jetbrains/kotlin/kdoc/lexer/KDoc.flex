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

DIGIT=[0-9]
LETTER = [:jletter:]
PLAIN_IDENTIFIER = {LETTER} ({LETTER} | {DIGIT})*
IDENTIFIER = {PLAIN_IDENTIFIER} | `[^`\n]+`
QUALIFIED_NAME = {IDENTIFIER} ([\.] {IDENTIFIER}?)* // Handle incorrect/incomplete qualifiers for correct resolving
CODE_LINK=\[{QUALIFIED_NAME}\]
CODE_FENCE_START=("```" | "~~~").*
// `org.jetbrains.kotlin.kdoc.lexer.KDocLexer` relies on these two types of ending fences.
// If this set is changed, please, update `KDocLexer` accordingly
CODE_FENCE_END=("```" | "~~~")

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

    "\\"[\[\]] {
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

    {CODE_FENCE_START} {
              lastBlockType = BlockType.Code;
              yybeginAndUpdate(CODE_BLOCK_LINE_BEGINNING);
              return KDocTokens.TEXT;
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
              // Code fence end
              yybeginAndUpdate(CONTENTS);
              return KDocTokens.TEXT;
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