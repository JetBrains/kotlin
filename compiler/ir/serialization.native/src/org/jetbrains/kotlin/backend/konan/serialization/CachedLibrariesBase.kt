/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.library.KotlinLibrary

data class SerializedFileReference(val fqName: String, val path: String) {
    constructor(irFile: IrFile) : this(irFile.packageFqName.asString(), irFile.path)
}

class SerializedInlineFunctionReference(
    val file: SerializedFileReference, val functionSignature: Int, val body: Int,
    val startOffset: Int, val endOffset: Int,
    val extensionReceiverSig: Int, val dispatchReceiverSig: Int, val outerReceiverSigs: IntArray,
    val valueParameterSigs: IntArray, val typeParameterSigs: IntArray,
    val defaultValues: IntArray,
)

// [binaryType] is needed in case a field is of a primitive type. Otherwise we know it's an object type and
// that is enough information for the backend.
class SerializedClassFieldInfo(val name: String, val binaryType: Int, val flags: Int, val alignment: Int) {
    companion object {
        const val FLAG_IS_CONST = 1
    }
}

class SerializedClassFields(
    val file: SerializedFileReference, val classSignature: IdSignature, val typeParameterSigs: IntArray,
    val outerThisIndex: Int, val fields: Array<SerializedClassFieldInfo>,
)

class SerializedEagerInitializedFile(val file: SerializedFileReference)

open class CachedLibrariesBase {
    open fun isLibraryCached(library: KotlinLibrary): Boolean = false

    open fun classesFields(library: KotlinLibrary): Map<IdSignature, SerializedClassFields> = error("Should not be called")

    open fun inlineFunctionReferences(
        library: KotlinLibrary,
        deserializeSignature: (SerializedInlineFunctionReference) -> IdSignature,
    ): Map<IdSignature, SerializedInlineFunctionReference> = error("Should not be called")

    open fun eagerInitializedFiles(
        library: KotlinLibrary,
        getFile: (SerializedEagerInitializedFile) -> IrFile,
    ): List<IrFile> = error("Should not be called")
}
