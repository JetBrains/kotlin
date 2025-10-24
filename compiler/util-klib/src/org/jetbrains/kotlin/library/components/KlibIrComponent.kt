/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.components

import org.jetbrains.kotlin.library.Klib
import org.jetbrains.kotlin.library.KlibComponentLayout
import org.jetbrains.kotlin.library.KlibOptionalComponent
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_FOLDER_NAME
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_INLINABLE_FUNCTIONS_FOLDER_NAME
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_DECLARATIONS_FILE_NAME
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_BODIES_FILE_NAME
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_TYPES_FILE_NAME
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_SIGNATURES_FILE_NAME
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_DEBUG_INFO_FILE_NAME
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_STRINGS_FILE_NAME
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_FILES_FILE_NAME
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_FILE_ENTRIES_FILE_NAME
import org.jetbrains.kotlin.library.impl.KLIB_DEFAULT_COMPONENT_NAME
import org.jetbrains.kotlin.konan.file.File as KlibFile

/**
 * This component that provides read access to Klib IR. It can be used for two purposes:
 * - Reading the (main) Klib IR. Corresponding component Kind: [Kind.Main].
 * - Reading the IR of inlinable functions. Corresponding component Kind: [Kind.InlinableFunctions].
 */
interface KlibIrComponent : KlibOptionalComponent {
    val irFileCount: Int

    fun irFile(index: Int): ByteArray
    fun irFileEntry(index: Int, fileIndex: Int): ByteArray?
    fun declaration(index: Int, fileIndex: Int): ByteArray
    fun body(index: Int, fileIndex: Int): ByteArray
    fun type(index: Int, fileIndex: Int): ByteArray
    fun signature(index: Int, fileIndex: Int): ByteArray
    fun signatureDebugInfo(index: Int, fileIndex: Int): ByteArray?
    fun stringLiteral(index: Int, fileIndex: Int): ByteArray

    fun irFileEntries(fileIndex: Int): ByteArray?
    fun declarations(fileIndex: Int): ByteArray
    fun bodies(fileIndex: Int): ByteArray
    fun types(fileIndex: Int): ByteArray
    fun signatures(fileIndex: Int): ByteArray
    fun stringLiterals(fileIndex: Int): ByteArray

    enum class Kind : KlibOptionalComponent.Kind<KlibIrComponent> { Main, InlinableFunctions }
}

/**
 * A shortcut for accessing the [KlibIrComponent] responsible for the main IR in the [Klib] instance.
 *
 * This component is optional: The [ir] getter returns `null` if there is no main IR in the library.
 *
 * Note: If you don't want to check the nullability of the returned value, use [irOrFail].
 */
inline val Klib.ir: KlibIrComponent?
    get() = getComponent(KlibIrComponent.Kind.Main)

/**
 * A shortcut for accessing the [KlibIrComponent] responsible for the main IR in the [Klib] instance.
 *
 * Note: In case there is no main IR in the library, this method will throw an exception.
 */
inline val Klib.irOrFail: KlibIrComponent
    get() = ir ?: error("No 'ir' component in library ${this.location}")

/**
 * A shortcut for accessing the [KlibIrComponent] responsible for inlinable functions in the [Klib] instance.
 *
 * This component is optional: The [inlinableFunctionsIr] getter returns `null` if there is no IR of inlinable functions in the library.
 */
inline val Klib.inlinableFunctionsIr: KlibIrComponent?
    get() = getComponent(KlibIrComponent.Kind.InlinableFunctions)

class KlibIrComponentLayout private constructor(root: KlibFile, private val irFolderName: String) : KlibComponentLayout(root) {
    /** The IR "home" directory. */
    val irDir: KlibFile
        get() = root.child(KLIB_DEFAULT_COMPONENT_NAME).child(irFolderName)

    /** The file with "IR files". */
    val irFilesFile: KlibFile
        get() = irDir.child(KLIB_IR_FILES_FILE_NAME)

    /** The file with "IR file entries". */
    val irFileEntriesFile: KlibFile
        get() = irDir.child(KLIB_IR_FILE_ENTRIES_FILE_NAME)

    /**
     * The file with all IR declarations.
     *
     * Important notes:
     * - Function and class constructor bodies are stored separately in [bodiesFile]. This makes it possible to
     *   load declarations without bodies when the compiler does not need them.
     * - Local declarations are stored together with functions bodies in [bodiesFile].
     */
    val declarationsFile: KlibFile
        get() = irDir.child(KLIB_IR_DECLARATIONS_FILE_NAME)

    /** The file with function bodies. */
    val bodiesFile: KlibFile
        get() = irDir.child(KLIB_IR_BODIES_FILE_NAME)

    /** The file with IR types. */
    val typesFile: KlibFile
        get() = irDir.child(KLIB_IR_TYPES_FILE_NAME)

    /** The file with IR signatures. */
    val signaturesFile: KlibFile
        get() = irDir.child(KLIB_IR_SIGNATURES_FILE_NAME)

    /** The file with the supplementary (debug) information about IR signatures. */
    val signaturesDebugInfoFile: KlibFile
        get() = irDir.child(KLIB_IR_DEBUG_INFO_FILE_NAME)

    /** The file with string literals. */
    val stringLiteralsFile: KlibFile
        get() = irDir.child(KLIB_IR_STRINGS_FILE_NAME)

    companion object {
        fun createForMainIr(root: KlibFile): KlibIrComponentLayout =
            KlibIrComponentLayout(root, KLIB_IR_FOLDER_NAME)

        fun createForInlinableFunctionsIr(root: KlibFile): KlibIrComponentLayout =
            KlibIrComponentLayout(root, KLIB_IR_INLINABLE_FUNCTIONS_FOLDER_NAME)
    }
}

object KlibIrConstants {
    const val KLIB_IR_FOLDER_NAME = "ir"
    const val KLIB_IR_INLINABLE_FUNCTIONS_FOLDER_NAME = "ir_inlinable_functions"

    const val KLIB_IR_DECLARATIONS_FILE_NAME = "irDeclarations.knd"
    const val KLIB_IR_TYPES_FILE_NAME = "types.knt"
    const val KLIB_IR_SIGNATURES_FILE_NAME = "signatures.knt"
    const val KLIB_IR_STRINGS_FILE_NAME = "strings.knt"
    const val KLIB_IR_BODIES_FILE_NAME = "bodies.knb"
    const val KLIB_IR_FILES_FILE_NAME = "files.knf"
    const val KLIB_IR_DEBUG_INFO_FILE_NAME = "debugInfo.knd"
    const val KLIB_IR_FILE_ENTRIES_FILE_NAME = "fileEntries.knf"
}
