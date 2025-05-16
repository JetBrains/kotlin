@file:Suppress(
    "PARAMETER_NAME_CHANGED_ON_OVERRIDE", // TODO: KT-77206 (Remove after fixing https://github.com/JetBrains/intellij-deps-jflex/issues/8)
    "PackageDirectoryMismatch", // The generated files are located in another directory
)

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
  private val isLastToken: Boolean
    get() = zzMarkedPos == zzBuffer.length

  private fun yytextContainLineBreaks(): Boolean {
    for (i in zzStartRead until zzMarkedPos) {
        if (zzBuffer[i].let { it == '\n' || it == '\r' }) return true
    }

    return false
  }
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

WHITE_SPACE_CHAR    =[\ \t\f\n]

DIGIT=[0-9]
ALPHA=[:jletter:]
TAG_NAME={ALPHA}({ALPHA}|{DIGIT})*
QUALIFIED_NAME_START={ALPHA}
QUALIFIED_NAME_CHAR={ALPHA}|{DIGIT}|[\.]
QUALIFIED_NAME={QUALIFIED_NAME_START}{QUALIFIED_NAME_CHAR}*
CODE_LINK=\[{QUALIFIED_NAME}\]
CODE_FENCE_START=("```" | "~~~").*
CODE_FENCE_END=("```" | "~~~")

%%


<YYINITIAL> "/**"                         {
            yybegin(CONTENTS_BEGINNING)
            return KDocTokens.START
}
"*"+ "/"                                  {
            return if (isLastToken) KDocTokens.END else KDocTokens.TEXT
}

<LINE_BEGINNING> "*"+                     {
            yybegin(CONTENTS_BEGINNING)
            return KDocTokens.LEADING_ASTERISK
}

<CONTENTS_BEGINNING> "@"{TAG_NAME} {
            val tag = KDocKnownTag.findByTagName(zzBuffer.subSequence(zzStartRead, zzMarkedPos))
            yybegin(if (tag != null && tag.isReferenceRequired) TAG_BEGINNING else TAG_TEXT_BEGINNING)
            return KDocTokens.TAG_NAME
}

<TAG_BEGINNING> {
    {WHITE_SPACE_CHAR}+ {
            if (yytextContainLineBreaks()) {
                yybegin(LINE_BEGINNING)
            }
            return SyntaxTokenTypes.WHITE_SPACE
    }

    /* Example: @return[x] The return value of function x
                       ^^^
    */
    {CODE_LINK} {
            yybegin(TAG_TEXT_BEGINNING)
            return KDocTokens.MARKDOWN_LINK
    }

    /* Example: @param aaa The value of aaa
                       ^^^
    */
    {QUALIFIED_NAME} {
            yybegin(TAG_TEXT_BEGINNING)
            return KDocTokens.MARKDOWN_LINK
    }

    [^\n] {
            yybegin(CONTENTS)
            return KDocTokens.TEXT
    }
}

<TAG_TEXT_BEGINNING> {
    {WHITE_SPACE_CHAR}+ {
            if (yytextContainLineBreaks()) {
                yybegin(LINE_BEGINNING)
            }
            return SyntaxTokenTypes.WHITE_SPACE
    }

    /* Example: @return[x] The return value of function x
                       ^^^
    */
    {CODE_LINK} {
            yybegin(CONTENTS)
            return KDocTokens.MARKDOWN_LINK
    }

    [^\n] {
            yybegin(CONTENTS)
            return KDocTokens.TEXT
    }
}

<LINE_BEGINNING, CONTENTS_BEGINNING, CONTENTS> {

    ([\ ]{4}[\ ]*)|([\t]+) {
            if(yystate() == CONTENTS_BEGINNING) {
                yybegin(INDENTED_CODE_BLOCK)
                return KDocTokens.CODE_BLOCK_TEXT
            }
    }

    {WHITE_SPACE_CHAR}+ {
            return if (yytextContainLineBreaks()) {
                yybegin(LINE_BEGINNING)
                SyntaxTokenTypes.WHITE_SPACE
            }  else {
                yybegin(if (yystate() == CONTENTS_BEGINNING) CONTENTS_BEGINNING else CONTENTS)
                KDocTokens.TEXT  // internal white space
            }
    }

    "\\"[\[\]] {
            yybegin(CONTENTS)
            return KDocTokens.MARKDOWN_ESCAPED_CHAR
    }

    "(" {
            yybegin(CONTENTS)
            return KDocTokens.KDOC_LPAR
    }

    ")" {
            yybegin(CONTENTS)
            return KDocTokens.KDOC_RPAR
    }

    {CODE_FENCE_START} {
            yybegin(CODE_BLOCK_LINE_BEGINNING)
            return KDocTokens.TEXT
    }

    /* We're only interested in parsing links that can become code references,
       meaning they contain only identifier characters and characters that can be
       used in type declarations. No brackets, backticks, asterisks or anything like that.
       Also if a link is followed by [ or (, then its destination is a regular HTTP
       link and not a Kotlin identifier, so we don't need to do our parsing and resolution. */
    {CODE_LINK} / [^\(\[] {
            yybegin(CONTENTS)
            return KDocTokens.MARKDOWN_LINK
    }

    [^\n] {
            yybegin(CONTENTS)
            return KDocTokens.TEXT
    }
}

<CODE_BLOCK_LINE_BEGINNING> {
    "*"+ {
            yybegin(CODE_BLOCK_CONTENTS_BEGINNING)
            return KDocTokens.LEADING_ASTERISK
    }
}

<CODE_BLOCK_LINE_BEGINNING, CODE_BLOCK_CONTENTS_BEGINNING> {
    {CODE_FENCE_END} / [ \t\f]* [\n] [ \t\f]* {
            // Code fence end
            yybegin(CONTENTS)
            return KDocTokens.TEXT
    }
}

<INDENTED_CODE_BLOCK, CODE_BLOCK_LINE_BEGINNING, CODE_BLOCK_CONTENTS_BEGINNING, CODE_BLOCK> {
    {WHITE_SPACE_CHAR}+ {
            if (yytextContainLineBreaks()) {
                yybegin(if (yystate() == INDENTED_CODE_BLOCK) LINE_BEGINNING else CODE_BLOCK_LINE_BEGINNING)
                return SyntaxTokenTypes.WHITE_SPACE
            }
            return KDocTokens.CODE_BLOCK_TEXT
    }

    [^\n] {
            yybegin(if (yystate() == INDENTED_CODE_BLOCK) INDENTED_CODE_BLOCK else CODE_BLOCK)
            return KDocTokens.CODE_BLOCK_TEXT
    }
}

[\s\S] { return SyntaxTokenTypes.BAD_CHARACTER }
