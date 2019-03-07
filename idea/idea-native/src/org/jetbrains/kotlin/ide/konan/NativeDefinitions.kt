/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.*
import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.fileTypes.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.tree.*
import javax.swing.Icon
import java.io.Reader
import org.jetbrains.kotlin.ide.konan.psi.*
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.konan.library.KDEFINITIONS_FILE_EXTENSION


const val NATIVE_DEFINITIONS_NAME = "KND"
const val NATIVE_DEFINITIONS_DESCRIPTION = "Definitions file for Kotlin/Native C interop"

object NativeDefinitionsFileType : LanguageFileType(NativeDefinitionsLanguage.INSTANCE) {

    override fun getName(): String = NATIVE_DEFINITIONS_NAME

    override fun getDescription(): String = NATIVE_DEFINITIONS_DESCRIPTION

    override fun getDefaultExtension(): String = KDEFINITIONS_FILE_EXTENSION

    override fun getIcon(): Icon = KotlinIcons.NATIVE
}

class NativeDefinitionsLanguage private constructor() : Language(NATIVE_DEFINITIONS_NAME) {
    companion object {
        val INSTANCE = NativeDefinitionsLanguage()
    }
}

class NativeDefinitionsFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, NativeDefinitionsLanguage.INSTANCE) {

    override fun getFileType(): FileType = NativeDefinitionsFileType

    override fun toString(): String = NATIVE_DEFINITIONS_DESCRIPTION

    override fun getIcon(flags: Int): Icon? = super.getIcon(flags)
}

class NativeDefinitionsLexerAdapter : FlexAdapter(NativeDefinitionsLexer(null as Reader?))

class NativeDefinitionsParserDefinition : ParserDefinition {
    private val COMMENTS = TokenSet.create(NativeDefinitionsTypes.COMMENT)
    private val FILE = IFileElementType(NativeDefinitionsLanguage.INSTANCE)

    override fun getWhitespaceTokens(): TokenSet = TokenSet.WHITE_SPACE
    override fun getCommentTokens(): TokenSet = COMMENTS
    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY
    override fun getFileNodeType(): IFileElementType = FILE

    override fun createLexer(project: Project): Lexer = NativeDefinitionsLexerAdapter()
    override fun createParser(project: Project): PsiParser = NativeDefinitionsParser()

    override fun createFile(viewProvider: FileViewProvider): PsiFile = NativeDefinitionsFile(viewProvider)
    override fun createElement(node: ASTNode): PsiElement = NativeDefinitionsTypes.Factory.createElement(node)

    @Suppress("OverridingDeprecatedMember") // Just switch to correctly named function, when old one is removed.
    override fun spaceExistanceTypeBetweenTokens(left: ASTNode?, right: ASTNode?): ParserDefinition.SpaceRequirements =
        ParserDefinition.SpaceRequirements.MAY
}

class CLanguageInjector : LanguageInjector {
    val cLanguage = Language.findLanguageByID("ObjectiveC")

    override fun getLanguagesToInject(host: PsiLanguageInjectionHost, registrar: InjectedLanguagePlaces) {
        if (!host.isValid) return

        if (host is NativeDefinitionsCodeImpl && cLanguage != null) {
            val range = host.getTextRange().shiftLeft(host.startOffsetInParent)
            registrar.addPlace(cLanguage, range, null, null)
        }
    }
}

object NativeDefinitionsSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> =
        when (tokenType) {
            TokenType.BAD_CHARACTER -> BAD_CHAR_KEYS
            NativeDefinitionsTypes.COMMENT -> COMMENT_KEYS
            NativeDefinitionsTypes.DELIM -> COMMENT_KEYS
            NativeDefinitionsTypes.SEPARATOR -> OPERATOR_KEYS
            NativeDefinitionsTypes.UNKNOWN_KEY -> BAD_CHAR_KEYS
            NativeDefinitionsTypes.UNKNOWN_PLATFORM -> BAD_CHAR_KEYS
            NativeDefinitionsTypes.VALUE -> VALUE_KEYS
            // known properties
            NativeDefinitionsTypes.COMPILER_OPTS -> KNOWN_PROPERTIES_KEYS
            NativeDefinitionsTypes.DEPENDS -> KNOWN_PROPERTIES_KEYS
            NativeDefinitionsTypes.DISABLE_DESIGNATED_INITIALIZER_CHECKS -> KNOWN_PROPERTIES_KEYS
            NativeDefinitionsTypes.ENTRY_POINT -> KNOWN_PROPERTIES_KEYS
            NativeDefinitionsTypes.EXCLUDE_DEPENDENT_MODULES -> KNOWN_PROPERTIES_KEYS
            NativeDefinitionsTypes.EXCLUDE_SYSTEM_LIBS -> KNOWN_PROPERTIES_KEYS
            NativeDefinitionsTypes.EXCLUDED_FUNCTIONS -> KNOWN_PROPERTIES_KEYS
            NativeDefinitionsTypes.EXCLUDED_MACROS -> KNOWN_PROPERTIES_KEYS
            NativeDefinitionsTypes.EXPORT_FORWARD_DECLARATIONS -> KNOWN_PROPERTIES_KEYS
            NativeDefinitionsTypes.HEADERS -> KNOWN_PROPERTIES_KEYS
            NativeDefinitionsTypes.HEADER_FILTER -> KNOWN_PROPERTIES_KEYS
            NativeDefinitionsTypes.LANGUAGE -> KNOWN_PROPERTIES_KEYS
            NativeDefinitionsTypes.LIBRARY_PATHS -> KNOWN_PROPERTIES_KEYS
            NativeDefinitionsTypes.LINKER -> KNOWN_PROPERTIES_KEYS
            NativeDefinitionsTypes.LINKER_OPTS -> KNOWN_PROPERTIES_KEYS
            NativeDefinitionsTypes.MODULES -> KNOWN_PROPERTIES_KEYS
            NativeDefinitionsTypes.NON_STRICT_ENUMS -> KNOWN_PROPERTIES_KEYS
            NativeDefinitionsTypes.NO_STRING_CONVERSION -> KNOWN_PROPERTIES_KEYS
            NativeDefinitionsTypes.PACKAGE -> KNOWN_PROPERTIES_KEYS
            NativeDefinitionsTypes.STATIC_LIBRARIES -> KNOWN_PROPERTIES_KEYS
            NativeDefinitionsTypes.STRICT_ENUMS -> KNOWN_PROPERTIES_KEYS
            // known extensions
            NativeDefinitionsTypes.ANDROID -> KNOWN_EXTENSIONS_KEYS
            NativeDefinitionsTypes.ANDROID_ARM32 -> KNOWN_EXTENSIONS_KEYS
            NativeDefinitionsTypes.ANDROID_ARM64 -> KNOWN_EXTENSIONS_KEYS
            NativeDefinitionsTypes.ARM32 -> KNOWN_EXTENSIONS_KEYS
            NativeDefinitionsTypes.ARM64 -> KNOWN_EXTENSIONS_KEYS
            NativeDefinitionsTypes.IOS -> KNOWN_EXTENSIONS_KEYS
            NativeDefinitionsTypes.IOS_ARM32 -> KNOWN_EXTENSIONS_KEYS
            NativeDefinitionsTypes.IOS_ARM64 -> KNOWN_EXTENSIONS_KEYS
            NativeDefinitionsTypes.IOS_X64 -> KNOWN_EXTENSIONS_KEYS
            NativeDefinitionsTypes.LINUX -> KNOWN_EXTENSIONS_KEYS
            NativeDefinitionsTypes.LINUX_ARM32_HFP -> KNOWN_EXTENSIONS_KEYS
            NativeDefinitionsTypes.LINUX_MIPS32 -> KNOWN_EXTENSIONS_KEYS
            NativeDefinitionsTypes.LINUX_MIPSEL32 -> KNOWN_EXTENSIONS_KEYS
            NativeDefinitionsTypes.LINUX_X64 -> KNOWN_EXTENSIONS_KEYS
            NativeDefinitionsTypes.MACOS_X64 -> KNOWN_EXTENSIONS_KEYS
            NativeDefinitionsTypes.MINGW -> KNOWN_EXTENSIONS_KEYS
            NativeDefinitionsTypes.MINGW_X64 -> KNOWN_EXTENSIONS_KEYS
            NativeDefinitionsTypes.MIPS32 -> KNOWN_EXTENSIONS_KEYS
            NativeDefinitionsTypes.MIPSEL32 -> KNOWN_EXTENSIONS_KEYS
            NativeDefinitionsTypes.OSX -> KNOWN_EXTENSIONS_KEYS
            NativeDefinitionsTypes.WASM -> KNOWN_EXTENSIONS_KEYS
            NativeDefinitionsTypes.WASM32 -> KNOWN_EXTENSIONS_KEYS
            NativeDefinitionsTypes.X64 -> KNOWN_EXTENSIONS_KEYS
            else -> EMPTY_KEYS
        }

    override fun getHighlightingLexer(): Lexer = NativeDefinitionsLexerAdapter()


    private fun createKeys(externalName: String, key: TextAttributesKey): Array<TextAttributesKey> {
        return arrayOf(TextAttributesKey.createTextAttributesKey(externalName, key))
    }

    private val BAD_CHAR_KEYS = createKeys("Unknown key", HighlighterColors.BAD_CHARACTER)
    private val COMMENT_KEYS = createKeys("Comment", DefaultLanguageHighlighterColors.LINE_COMMENT)
    private val EMPTY_KEYS = emptyArray<TextAttributesKey>()
    private val KNOWN_EXTENSIONS_KEYS = createKeys("Known extension", DefaultLanguageHighlighterColors.LABEL)
    private val KNOWN_PROPERTIES_KEYS = createKeys("Known property", DefaultLanguageHighlighterColors.KEYWORD)
    private val OPERATOR_KEYS = createKeys("Operator", DefaultLanguageHighlighterColors.OPERATION_SIGN)
    private val VALUE_KEYS = createKeys("Value", DefaultLanguageHighlighterColors.STRING)
}

class NativeDefinitionsSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
        NativeDefinitionsSyntaxHighlighter
}