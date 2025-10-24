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

  /**
   * It should be used by default instead of [yybegin] except in cases where manual handling of [consecutiveLineBreakCount] is needed.
   * For instance, although the leading asterisk switches to the [CONTENTS_BEGINNING] state, it shouldn't affect [consecutiveLineBreakCount]
   * because it's a special char that's invisible from the point of view of the plain Markdown parser.
   */
  private fun yybeginAndUpdate(newState: Int) {
    consecutiveLineBreakCount = if (newState == LINE_BEGINNING || newState == CODE_BLOCK_LINE_BEGINNING) consecutiveLineBreakCount + 1 else 0
    yybegin(newState)
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
// `org.jetbrains.kotlin.kmp.lexer.KDocLexer` relies on these two types of ending fences.
// If this set is changed, please, update `KDocLexer` accordingly
CODE_FENCE_END=("```" | "~~~")

%%


<YYINITIAL> "/**"                         {
            yybeginAndUpdate(CONTENTS_BEGINNING)
            return KDocTokens.START
}

"*"+ "/" {
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
            lastBlockType = BlockType.Paragraph
            val tag = KDocKnownTag.findByTagName(zzBuffer.subSequence(zzStartRead, zzMarkedPos))
            yybeginAndUpdate(if (tag != null && tag.isReferenceRequired) TAG_BEGINNING else TAG_TEXT_BEGINNING)
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
            yybeginAndUpdate(TAG_TEXT_BEGINNING)
            return KDocTokens.MARKDOWN_LINK
    }

    /* Example: @param aaa The value of aaa
                       ^^^
    */
    {QUALIFIED_NAME} {
            yybeginAndUpdate(TAG_TEXT_BEGINNING)
            return KDocTokens.MARKDOWN_LINK
    }

    [^] {
            yybeginAndUpdate(CONTENTS)
            return KDocTokens.TEXT
    }
}

<TAG_TEXT_BEGINNING> {
    {LINE_BREAK_CHAR} {
            yybeginAndUpdate(LINE_BEGINNING)
            return SyntaxTokenTypes.WHITE_SPACE
    }

    {WHITE_SPACE_CHAR}+ {
            return SyntaxTokenTypes.WHITE_SPACE
    }

    /* Example: @return[x] The return value of function x
                       ^^^
    */
    {CODE_LINK} {
            yybeginAndUpdate(CONTENTS)
            return KDocTokens.MARKDOWN_LINK
    }

    [^] {
            yybeginAndUpdate(CONTENTS)
            return KDocTokens.TEXT
    }
}

<LINE_BEGINNING, CONTENTS_BEGINNING, CONTENTS> {
    {LINE_BREAK_CHAR} {
            yybeginAndUpdate(LINE_BEGINNING)
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

            yybegin(if (yystate() == CONTENTS_BEGINNING) CONTENTS_BEGINNING else CONTENTS)

            return KDocTokens.TEXT  // internal white space
    }

    "\\"[\[\]] {
            lastBlockType = BlockType.Paragraph
            yybeginAndUpdate(CONTENTS)
            return KDocTokens.MARKDOWN_ESCAPED_CHAR
    }

    "(" {
            lastBlockType = BlockType.Paragraph
            yybeginAndUpdate(CONTENTS)
            return KDocTokens.KDOC_LPAR
    }

    ")" {
            lastBlockType = BlockType.Paragraph
            yybeginAndUpdate(CONTENTS)
            return KDocTokens.KDOC_RPAR
    }

    {CODE_FENCE_START} {
            lastBlockType = BlockType.Code
            yybeginAndUpdate(CODE_BLOCK_LINE_BEGINNING)
            return KDocTokens.TEXT
    }

    /* We're only interested in parsing links that can become code references,
       meaning they contain only identifier characters and characters that can be
       used in type declarations. No brackets, backticks, asterisks or anything like that.
       Also if a link is followed by [ or (, then its destination is a regular HTTP
       link and not a Kotlin identifier, so we don't need to do our parsing and resolution. */
    {CODE_LINK} / [^\(\[] {
            lastBlockType = BlockType.Paragraph
            yybeginAndUpdate(CONTENTS)
            return KDocTokens.MARKDOWN_LINK
    }

    [^] {
            lastBlockType = BlockType.Paragraph
            yybeginAndUpdate(CONTENTS)
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
            // Code fence end
            yybeginAndUpdate(CONTENTS)
            return KDocTokens.TEXT
    }
}

<INDENTED_CODE_BLOCK, CODE_BLOCK_LINE_BEGINNING, CODE_BLOCK_CONTENTS_BEGINNING, CODE_BLOCK> {
    {LINE_BREAK_CHAR} {
            yybegin(if (yystate() == INDENTED_CODE_BLOCK) LINE_BEGINNING else CODE_BLOCK_LINE_BEGINNING)
            return SyntaxTokenTypes.WHITE_SPACE
    }

    {WHITE_SPACE_CHAR}+ {
            return KDocTokens.CODE_BLOCK_TEXT
    }

    [^] {
            yybeginAndUpdate(if (yystate() == INDENTED_CODE_BLOCK) INDENTED_CODE_BLOCK else CODE_BLOCK)
            return KDocTokens.CODE_BLOCK_TEXT
    }
}

[\s\S] {
            lastBlockType = BlockType.Paragraph
            return SyntaxTokenTypes.BAD_CHARACTER
}