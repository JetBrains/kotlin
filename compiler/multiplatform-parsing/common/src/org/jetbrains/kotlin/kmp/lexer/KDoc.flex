@file:Suppress("PackageDirectoryMismatch") // The generated files are located in another directory

package org.jetbrains.kotlin.kmp.lexer

import com.intellij.platform.syntax.SyntaxElementType
import com.intellij.platform.syntax.element.SyntaxTokenTypes
import com.intellij.platform.syntax.util.lexer.FlexLexer
import org.jetbrains.annotations.ApiStatus
import kotlin.jvm.JvmStatic // Not needed on JVM, but needed when compiling other targets

@ApiStatus.Experimental
%%

%unicode
%class KDocFlexLexer
%implements FlexLexer

%{
  /**
   * Counts the number of line breaks after the previous text, typically paragraph.
   * White spaces as well as leading asterisks aren't considered as text, so, they don't reset the counter.
   * It allows implementing markdown spec in a more convenient way.
   * For instance, indented code blocks require two consecutive line breaks after paragraphs.
   */
  private var consecutiveLineBreakCount: Int = 0

  private var lastBlockType: BlockType? = null

  private enum class BlockType {
      Paragraph,
      Code,
  }

  private val isLastToken: Boolean
    get() = zzMarkedPos == zzBuffer.length
%}

%function advance
%type SyntaxElementType

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
CODE_FENCE_END=("```" | "~~~")

%%


<YYINITIAL> "/**"                         {
            yybegin(CONTENTS_BEGINNING)
            return KDocTokens.START
}

"*"+ "/" {
            consecutiveLineBreakCount = 0
            return if (isLastToken) KDocTokens.END else KDocTokens.TEXT
}

<LINE_BEGINNING> {
    {WHITE_SPACE_CHAR}+ {
            return SyntaxTokenTypes.WHITE_SPACE
    }
    "*"+ {
            yybegin(CONTENTS_BEGINNING)
            return KDocTokens.LEADING_ASTERISK
    }
}

<CONTENTS_BEGINNING> "@"{PLAIN_IDENTIFIER} {
            consecutiveLineBreakCount = 0
            lastBlockType = BlockType.Paragraph
            val tag = KDocKnownTag.findByTagName(zzBuffer.subSequence(zzStartRead, zzMarkedPos))
            yybegin(if (tag != null && tag.isReferenceRequired) TAG_BEGINNING else TAG_TEXT_BEGINNING)
            return KDocTokens.TAG_NAME
}

<TAG_BEGINNING> {
    {LINE_BREAK_CHAR} {
            consecutiveLineBreakCount++
            yybegin(LINE_BEGINNING)
            return SyntaxTokenTypes.WHITE_SPACE
    }

    {WHITE_SPACE_CHAR}+ {
            return SyntaxTokenTypes.WHITE_SPACE
    }

    /* Example: @return[x] The return value of function x
                       ^^^
    */
    {CODE_LINK} {
            consecutiveLineBreakCount = 0
            yybegin(TAG_TEXT_BEGINNING)
            return KDocTokens.MARKDOWN_LINK
    }

    /* Example: @param aaa The value of aaa
                       ^^^
    */
    {QUALIFIED_NAME} {
            consecutiveLineBreakCount = 0
            yybegin(TAG_TEXT_BEGINNING)
            return KDocTokens.MARKDOWN_LINK
    }

    [^] {
            consecutiveLineBreakCount = 0
            yybegin(CONTENTS)
            return KDocTokens.TEXT
    }
}

<TAG_TEXT_BEGINNING> {
    {LINE_BREAK_CHAR} {
            consecutiveLineBreakCount++
            yybegin(LINE_BEGINNING)
            return SyntaxTokenTypes.WHITE_SPACE
    }

    {WHITE_SPACE_CHAR}+ {
            return SyntaxTokenTypes.WHITE_SPACE
    }

    /* Example: @return[x] The return value of function x
                       ^^^
    */
    {CODE_LINK} {
            consecutiveLineBreakCount = 0
            yybegin(CONTENTS)
            return KDocTokens.MARKDOWN_LINK
    }

    [^] {
            consecutiveLineBreakCount = 0
            yybegin(CONTENTS)
            return KDocTokens.TEXT
    }
}

<LINE_BEGINNING, CONTENTS_BEGINNING, CONTENTS> {
    {LINE_BREAK_CHAR} {
            consecutiveLineBreakCount++
            yybegin(LINE_BEGINNING)
            return SyntaxTokenTypes.WHITE_SPACE
    }

    {WHITE_SPACE_CHAR}+ {
            val state = yystate()

            if (
                // Recognize indented code blocks if only the line starts with an asterisk(s)
                state == CONTENTS_BEGINNING &&

                // If there are more than 4 spaces at the beginning of the line or a tab char, we are trying to recognize indented code block
                (zzMarkedPos - zzStartRead >= 4 || zzBuffer[zzStartRead] == '\t' || zzBuffer[zzMarkedPos - 1] == '\t') &&

                // If the last block type is paragraph, more than 1 consecutive line break is required
                (lastBlockType != BlockType.Paragraph || consecutiveLineBreakCount >= 2)
            ) {
                yybegin(INDENTED_CODE_BLOCK)
                lastBlockType = BlockType.Code
                return KDocTokens.CODE_BLOCK_TEXT
            }

            if (state != CONTENTS_BEGINNING) {
                yybegin(CONTENTS)
            }

            return KDocTokens.TEXT  // internal white space
    }

    "\\"[\[\]] {
            consecutiveLineBreakCount = 0
            lastBlockType = BlockType.Paragraph
            yybegin(CONTENTS)
            return KDocTokens.MARKDOWN_ESCAPED_CHAR
    }

    "(" {
            consecutiveLineBreakCount = 0
            lastBlockType = BlockType.Paragraph
            yybegin(CONTENTS)
            return KDocTokens.KDOC_LPAR
    }

    ")" {
            consecutiveLineBreakCount = 0
            lastBlockType = BlockType.Paragraph
            yybegin(CONTENTS)
            return KDocTokens.KDOC_RPAR
    }

    {CODE_FENCE_START} {
            consecutiveLineBreakCount = 0
            lastBlockType = BlockType.Code
            yybegin(CODE_BLOCK_LINE_BEGINNING)
            return KDocTokens.TEXT
    }

    /* We're only interested in parsing links that can become code references,
       meaning they contain only identifier characters and characters that can be
       used in type declarations. No brackets, backticks, asterisks or anything like that.
       Also if a link is followed by [ or (, then its destination is a regular HTTP
       link and not a Kotlin identifier, so we don't need to do our parsing and resolution. */
    {CODE_LINK} / [^\(\[] {
            consecutiveLineBreakCount = 0
            lastBlockType = BlockType.Paragraph
            yybegin(CONTENTS)
            return KDocTokens.MARKDOWN_LINK
    }

    [^] {
            consecutiveLineBreakCount = 0
            lastBlockType = BlockType.Paragraph
            yybegin(CONTENTS)
            return KDocTokens.TEXT
    }
}

<CODE_BLOCK_LINE_BEGINNING> {
    {WHITE_SPACE_CHAR}+ {
            return SyntaxTokenTypes.WHITE_SPACE
    }

    "*"+ {
            yybegin(CODE_BLOCK_CONTENTS_BEGINNING)
            return KDocTokens.LEADING_ASTERISK
    }
}

<CODE_BLOCK_LINE_BEGINNING, CODE_BLOCK_CONTENTS_BEGINNING> {
    {CODE_FENCE_END} / [ \t\f]* [\n] {
            consecutiveLineBreakCount = 0
            // Code fence end
            yybegin(CONTENTS)
            return KDocTokens.TEXT
    }
}

<INDENTED_CODE_BLOCK, CODE_BLOCK_LINE_BEGINNING, CODE_BLOCK_CONTENTS_BEGINNING, CODE_BLOCK> {
    {LINE_BREAK_CHAR} {
            consecutiveLineBreakCount++
            yybegin(if (yystate() == INDENTED_CODE_BLOCK) LINE_BEGINNING else CODE_BLOCK_LINE_BEGINNING)
            return SyntaxTokenTypes.WHITE_SPACE
    }

    {WHITE_SPACE_CHAR}+ {
            return KDocTokens.CODE_BLOCK_TEXT
    }

    [^] {
            consecutiveLineBreakCount = 0
            if (yystate() != INDENTED_CODE_BLOCK) {
                yybegin(CODE_BLOCK)
            }
            return KDocTokens.CODE_BLOCK_TEXT
    }
}

[\s\S] {
            consecutiveLineBreakCount = 0
            lastBlockType = BlockType.Paragraph
            return SyntaxTokenTypes.BAD_CHARACTER
}