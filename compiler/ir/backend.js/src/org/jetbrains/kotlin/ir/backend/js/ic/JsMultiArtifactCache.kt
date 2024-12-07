/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.*
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.CodedOutputStream
import java.io.File

abstract class JsMultiArtifactCache<T : JsMultiArtifactCache.CacheInfo> {
    abstract fun loadProgramHeadersFromCache(): List<T>
    abstract fun loadRequiredJsIrModules(crossModuleReferences: Map<JsIrModuleHeader, CrossModuleReferences>)
    abstract fun fetchCompiledJsCode(cacheInfo: T): CompilationOutputsCached?
    abstract fun loadJsIrModule(cacheInfo: T): JsIrModule
    abstract fun commitCompiledJsCode(cacheInfo: T, compilationOutputs: CompilationOutputsBuilt): CompilationOutputs

    protected fun File.writeIfNotNull(data: String?) {
        if (data != null) {
            parentFile?.mkdirs()
            writeText(data)
        } else {
            delete()
        }
    }

    protected fun CodedInputStream.fetchJsIrModuleHeaderNames(): JsIrModuleHeaderNames {
        val definitions = mutableSetOf<String>()
        val nameBindings = mutableMapOf<String, String>()
        val optionalCrossModuleImports = hashSetOf<String>()

        repeat(readInt32()) {
            val tag = readString()
            val mask = readInt32()
            if (mask and NameType.DEFINITIONS.typeMask != 0) {
                definitions += tag
            }
            if (mask and NameType.OPTIONAL_IMPORTS.typeMask != 0) {
                optionalCrossModuleImports += tag
            }
            if (mask and NameType.NAME_BINDINGS.typeMask != 0) {
                nameBindings[tag] = readString()
            }
        }

        return JsIrModuleHeaderNames(definitions, nameBindings, optionalCrossModuleImports)
    }

    protected fun CodedOutputStream.commitJsIrModuleHeaderNames(jsIrHeader: JsIrModuleHeader) {
        val names = mutableMapOf<String, Pair<Int, String?>>()

        for ((tag, name) in jsIrHeader.nameBindings) {
            names[tag] = NameType.NAME_BINDINGS.typeMask to name
        }
        for (tag in jsIrHeader.optionalCrossModuleImports) {
            val maskAndName = names[tag]
            names[tag] = ((maskAndName?.first ?: 0) or NameType.OPTIONAL_IMPORTS.typeMask) to maskAndName?.second
        }
        for (tag in jsIrHeader.definitions) {
            val maskAndName = names[tag]
            names[tag] = ((maskAndName?.first ?: 0) or NameType.DEFINITIONS.typeMask) to maskAndName?.second
        }

        writeInt32NoTag(names.size)

        for ((tag, maskAndName) in names) {
            writeStringNoTag(tag)
            writeInt32NoTag(maskAndName.first)
            if (maskAndName.second != null) {
                writeStringNoTag(maskAndName.second)
            }
        }
    }

    interface CacheInfo {
        val jsIrHeader: JsIrModuleHeader
    }

    protected data class JsIrModuleHeaderNames(
        val definitions: Set<String>,
        val nameBindings: Map<String, String>,
        val optionalCrossModuleImports: Set<String>,
    )

    protected enum class NameType(val typeMask: Int) {
        DEFINITIONS(0b1),
        NAME_BINDINGS(0b10),
        OPTIONAL_IMPORTS(0b100)
    }
}
