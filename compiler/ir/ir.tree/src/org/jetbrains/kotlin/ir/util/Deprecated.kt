/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.visitors.acceptVoid

@Deprecated(
    "Use the overload with DumpIrTreeOptions instead.",
    ReplaceWith(
        "dump(DumpIrTreeOptions(normalizeNames = normalizeNames, stableOrder = stableOrder))",
        "org.jetbrains.kotlin.ir.util.DumpIrTreeOptions",
    ),
    DeprecationLevel.ERROR,
)
fun IrElement.dump(normalizeNames: Boolean = false, stableOrder: Boolean = false) =
    dump(DumpIrTreeOptions(normalizeNames = normalizeNames, stableOrder = stableOrder))

@Deprecated(
    "Use the overload with DumpIrTreeOptions instead.",
    ReplaceWith(
        "dumpTreesFromLineNumber(lineNumber, DumpIrTreeOptions(normalizeNames = normalizeNames))",
        "org.jetbrains.kotlin.ir.util.DumpIrTreeOptions",
    ),
    DeprecationLevel.ERROR,
)
fun IrFile.dumpTreesFromLineNumber(lineNumber: Int, normalizeNames: Boolean = false) =
    dumpTreesFromLineNumber(lineNumber, DumpIrTreeOptions(normalizeNames = normalizeNames))

@Deprecated(
    "Use the overload with DumpIrTreeOptions instead.",
    ReplaceWith(
        "DumpTreeFromSourceLineVisitor(fileEntry, lineNumber, out, DumpIrTreeOptions(normalizeNames = normalizeNames))",
        "org.jetbrains.kotlin.ir.util.DumpIrTreeOptions",
    ),
    DeprecationLevel.ERROR,
)
fun DumpTreeFromSourceLineVisitor(fileEntry: IrFileEntry, lineNumber: Int, out: Appendable, normalizeNames: Boolean = false) =
    DumpTreeFromSourceLineVisitor(fileEntry, lineNumber, out, DumpIrTreeOptions(normalizeNames = normalizeNames))

@Deprecated(
    "Use the overload with DumpIrTreeOptions instead.",
    ReplaceWith(
        "DumpIrTreeVisitor(out, DumpIrTreeOptions(normalizeNames = normalizeNames, stableOrder = stableOrder))",
        "org.jetbrains.kotlin.ir.util.DumpIrTreeOptions",
    ),
    DeprecationLevel.ERROR,
)
fun DumpIrTreeVisitor(out: Appendable, normalizeNames: Boolean = false, stableOrder: Boolean = false) =
    DumpIrTreeVisitor(out, DumpIrTreeOptions(normalizeNames = normalizeNames, stableOrder = stableOrder))

@Deprecated(
    "Use the overload with DumpIrTreeOptions instead.",
    ReplaceWith(
        "RenderIrElementVisitor(DumpIrTreeOptions(normalizeNames = normalizeNames, stableOrder = !verboseErrorTypes))",
        "org.jetbrains.kotlin.ir.util.DumpIrTreeOptions",
    ),
    DeprecationLevel.ERROR,
)
fun RenderIrElementVisitor(normalizeNames: Boolean = false, verboseErrorTypes: Boolean = true) =
    RenderIrElementVisitor(DumpIrTreeOptions(normalizeNames = normalizeNames, verboseErrorTypes = verboseErrorTypes))

// This class is not used meaningfully, but is left for compatibility with compose.
abstract class SymbolRenamer private constructor() {
    @Deprecated("Used from Compose.")
    companion object DEFAULT : SymbolRenamer()
}

// This member is left for compatibility with compose.
@Deprecated("Use the other deepCopyWithSymbols instead.")
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution // To fix bootstrap with K1 which cannot distinguish between the other deepCopyWithSymbols by lambda type.
inline fun <reified T : IrElement> T.deepCopyWithSymbols(
    initialParent: IrDeclarationParent?,
    createCopier: (SymbolRemapper, TypeRemapper) -> DeepCopyIrTreeWithSymbols,
): T {
    val symbolRemapper = DeepCopySymbolRemapper()
    acceptVoid(symbolRemapper)
    return transform(createCopier(symbolRemapper, DeepCopyTypeRemapper(symbolRemapper)), null).patchDeclarationParents(initialParent) as T
}
