/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils.serialization

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIrClassModel
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIrIcClassModel
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIrProgramFragment
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.serialization.js.ast.JsAstProtoBuf.*
import org.jetbrains.kotlin.serialization.js.ast.JsAstSerializerBase
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.io.OutputStream

class JsIrAstSerializer: JsAstSerializerBase() {

    fun serialize(fragment: JsIrProgramFragment, output: OutputStream) {
        importedNames.clear()
        importedNames += fragment.imports.map { fragment.nameBindings[it.key]!! }
        serialize(fragment).writeTo(output)
    }

    fun serialize(fragment: JsIrProgramFragment): Chunk {
        try {
            val chunkBuilder = Chunk.newBuilder()
            chunkBuilder.fragment = serializeFragment(fragment)
            chunkBuilder.nameTable = nameTableBuilder.build()
            chunkBuilder.stringTable = stringTableBuilder.build()
            return chunkBuilder.build()
        } finally {
            nameTableBuilder.clear()
            stringTableBuilder.clear()
            nameMap.clear()
            stringMap.clear()
        }
    }

    private fun serializeFragment(fragment: JsIrProgramFragment): Fragment {
        val fragmentBuilder = Fragment.newBuilder()

        fragmentBuilder.packageFqn = fragment.packageFqn

        for (importedModule in fragment.importedModules) {
            val importedModuleBuilder = ImportedModule.newBuilder()
            importedModuleBuilder.externalNameId = serialize(importedModule.externalName)
            importedModuleBuilder.internalNameId = serialize(importedModule.internalName)
            importedModule.plainReference?.let { importedModuleBuilder.plainReference = serialize(it) }
            fragmentBuilder.addImportedModule(importedModuleBuilder)
        }

        for ((signature, expression) in fragment.imports) {
            val importBuilder = Import.newBuilder()
            importBuilder.signatureId = serialize(signature)
            importBuilder.expression = serialize(expression)
            fragmentBuilder.addImportEntry(importBuilder)
        }

        fragmentBuilder.declarationBlock = serializeBlock(fragment.declarations)
        fragmentBuilder.initializerBlock = serializeBlock(fragment.initializers)
        fragmentBuilder.exportBlock = serializeBlock(fragment.exports)

        for ((key, name) in fragment.nameBindings.entries) {
            val nameBindingBuilder = NameBinding.newBuilder()
            nameBindingBuilder.signatureId = serialize(key)
            nameBindingBuilder.nameId = serialize(name)
            fragmentBuilder.addNameBinding(nameBindingBuilder)
        }

        fragment.classes.entries.forEach { (name, model) -> fragmentBuilder.addIrClassModel(serialize(name, model)) }

        fragment.testFunInvocation?.let {
            fragmentBuilder.setTestsInvocation(serialize(it))
        }

        fragment.mainFunction?.let {
            fragmentBuilder.setMainInvocation(serialize(it))
        }

        fragment.dts?.let {
            fragmentBuilder.dts = it
        }

        fragment.suiteFn?.let {
            fragmentBuilder.setSuiteFunction(serialize(it))
        }

        fragment.definitions.forEach {
            fragmentBuilder.addDefinitions(serialize(it))
        }

        fragmentBuilder.addAllPolyfills(fragment.polyfills)

        return fragmentBuilder.build()
    }

    private fun serialize(name: JsName, classModel: JsIrIcClassModel): IrClassModel {
        val builder = IrClassModel.newBuilder()
        builder.nameId = serialize(name)
        classModel.superClasses.forEach { builder.addSuperClasses(serialize(it)) }
        if (classModel.preDeclarationBlock.statements.isNotEmpty()) {
            builder.preDeclarationBlock = serializeBlock(classModel.preDeclarationBlock)
        }
        if (classModel.postDeclarationBlock.statements.isNotEmpty()) {
            builder.postDeclarationBlock = serializeBlock(classModel.postDeclarationBlock)
        }
        return builder.build()
    }

    override fun extractLocation(node: JsNode): JsLocation? {
        return node.source.safeAs<JsLocationWithSource>()?.asSimpleLocation()
    }
}
