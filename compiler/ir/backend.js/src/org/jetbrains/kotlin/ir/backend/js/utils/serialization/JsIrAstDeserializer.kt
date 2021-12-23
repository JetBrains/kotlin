/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils.serialization

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIrIcClassModel
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIrProgramFragment
import org.jetbrains.kotlin.ir.backend.js.utils.emptyScope
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.JsImportedModule
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.serialization.js.ast.JsAstDeserializerBase
import org.jetbrains.kotlin.serialization.js.ast.JsAstProtoBuf.*
import java.io.InputStream

class JsIrAstDeserializer : JsAstDeserializerBase() {
    override val scope = emptyScope

    fun deserialize(input: InputStream): JsIrProgramFragment {
        return deserialize(Chunk.parseFrom(CodedInputStream.newInstance(input).apply { setRecursionLimit(4096) }))
    }

    fun deserialize(proto: Chunk): JsIrProgramFragment {
        stringTable += proto.stringTable.entryList
        nameTable += proto.nameTable.entryList
        nameCache += nameTable.map { null }
        try {
            return deserialize(proto.fragment)
        } finally {
            stringTable.clear()
            nameTable.clear()
            nameCache.clear()
        }
    }

    private fun deserialize(proto: Fragment): JsIrProgramFragment {
        val fragment = JsIrProgramFragment(proto.packageFqn)

        fragment.importedModules += proto.importedModuleList.map { importedModuleProto ->
            JsImportedModule(
                deserializeString(importedModuleProto.externalNameId),
                deserializeName(importedModuleProto.internalNameId),
                if (importedModuleProto.hasPlainReference()) deserialize(importedModuleProto.plainReference) else null
            )
        }

        proto.importEntryList.associateTo(fragment.imports) { importProto ->
            deserializeString(importProto.signatureId) to deserialize(importProto.expression)
        }

        if (proto.hasDeclarationBlock()) {
            fragment.declarations.statements += deserializeGlobalBlock(proto.declarationBlock).statements
        }
        if (proto.hasInitializerBlock()) {
            fragment.initializers.statements += deserializeGlobalBlock(proto.initializerBlock).statements
        }
        if (proto.hasExportBlock()) {
            fragment.exports.statements += deserializeGlobalBlock(proto.exportBlock).statements
        }
        if (proto.hasPolyfills()) {
            fragment.polyfills.statements += deserializeGlobalBlock(proto.polyfills).statements
        }

        proto.nameBindingList.associateTo(fragment.nameBindings) { nameBindingProto ->
            deserializeString(nameBindingProto.signatureId) to deserializeName(nameBindingProto.nameId)
        }

        proto.irClassModelList.associateTo(fragment.classes) { clsProto -> deserialize(clsProto) }

        if (proto.hasTestsInvocation()) {
            fragment.testFunInvocation = deserialize(proto.testsInvocation)
        }

        if (proto.hasMainInvocation()) {
            fragment.mainFunction = deserialize(proto.mainInvocation)
        }

        if (proto.hasSuiteFunction()) {
            fragment.suiteFn = deserializeName(proto.suiteFunction)
        }

        fragment.dts = proto.dts
        fragment.definitions += proto.definitionsList.map { deserializeString(it) }
        fragment.polyfills = JsPolyfills(proto.polyfillsList)

        return fragment
    }

    private fun deserialize(proto: IrClassModel): Pair<JsName, JsIrIcClassModel> {
        return deserializeName(proto.nameId) to JsIrIcClassModel(proto.superClassesList.map { deserializeName(it) }).apply {
            if (proto.hasPreDeclarationBlock()) {
                preDeclarationBlock.statements += deserializeGlobalBlock(proto.preDeclarationBlock).statements
            }
            if (proto.hasPostDeclarationBlock()) {
                postDeclarationBlock.statements += deserializeGlobalBlock(proto.postDeclarationBlock).statements
            }
        }
    }

    override fun embedSources(deserializedLocation: JsLocation, file: String): JsLocationWithSource? = null
}
