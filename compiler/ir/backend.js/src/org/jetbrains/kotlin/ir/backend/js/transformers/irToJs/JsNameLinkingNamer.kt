/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.constant
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class JsNameLinkingNamer(
    private val context: JsIrBackendContext,
    private val minimizedMemberNames: Boolean,
    private val isEsModules: Boolean
) : IrNamerBase() {

    val nameMap = mutableMapOf<IrDeclaration, JsName>()

    private fun IrDeclarationWithName.getName(): JsName {
        return nameMap.getOrPut(this) {
            val name = (this as? IrClass)?.let { context.localClassNames[this] } ?: let {
                this.nameIfPropertyAccessor() ?: getJsNameOrKotlinName().asString()
            }
            JsName(sanitizeName(name), true)
        }
    }

    val importedModules = mutableListOf<JsImportedModule>()
    val imports = mutableMapOf<IrDeclaration, JsStatement>()

    override fun getNameForStaticDeclaration(declaration: IrDeclarationWithName): JsName {
        if (declaration.isEffectivelyExternal()) {
            val jsModule: String? = declaration.getJsModule()
            val maybeParentFile: IrFile? = declaration.parent as? IrFile
            val fileJsModule: String? = maybeParentFile?.getJsModule()
            val jsQualifier: List<JsName>? = maybeParentFile?.getJsQualifier()?.split('.')?.map { JsName(it, false) }

            return when {
                jsModule != null -> declaration.generateImportForDeclarationWithJsModule(jsModule)
                fileJsModule != null -> declaration.generateImportForDeclarationInFileWithJsModule(fileJsModule, jsQualifier)
                else -> declaration.generateRegularQualifiedImport(jsQualifier)
            }
        }

        return declaration.getName().also {
            if (declaration == context.intrinsics.void.owner.backingField) {
                it.constant = true
            }
        }
    }

    override fun getNameForMemberFunction(function: IrSimpleFunction): JsName {
        require(function.dispatchReceiverParameter != null)
        val signature = jsFunctionSignature(function, context)
        if (context.keeper.shouldKeep(function)) {
            context.minimizedNameGenerator.keepName(signature)
        }
        val result = if (minimizedMemberNames && !function.hasStableJsName(context)) {
            function.parentAsClass.fieldData()
            context.minimizedNameGenerator.nameBySignature(signature)
        } else {
            signature
        }
        return result.toJsName()
    }

    override fun getNameForMemberField(field: IrField): JsName {
        require(!field.isStatic)
        // TODO this looks funny. Rethink.
        return JsName(field.parentAsClass.fieldData()[field]!!, false)
    }

    private fun IrDeclarationWithName.generateImportForDeclarationWithJsModule(jsModule: String): JsName {
        val nameString = if (isJsNonModule()) {
            getJsNameOrKotlinName().asString()
        } else {
            val parent = fqNameWhenAvailable!!.parent()
            parent.child(getJsNameOrKotlinName()).asString()
        }
        val name = JsName(sanitizeName(nameString), true)
        val nameRef = name.makeRef()

        if (isEsModules) {
            val importSubject = when {
                this is IrClass && isObject -> JsImport.Target.All(nameRef)
                else -> JsImport.Target.Default(nameRef)
            }
            imports[this] = JsImport(jsModule, importSubject)
            nameMap[this] = name
        } else {
            importedModules += JsImportedModule(jsModule, name, nameRef)
        }
        return name
    }

    private fun IrDeclarationWithName.generateImportForDeclarationInFileWithJsModule(
        fileJsModule: String,
        jsQualifier: List<JsName>?
    ): JsName {
        if (this in nameMap) return getName()

        val declarationStableName = getJsNameOrKotlinName().identifier

        if (isEsModules) {
            val importedName = jsQualifier?.firstOrNull() ?: declarationStableName.toJsName(temporary = false)
            val importStatement = JsImport(fileJsModule, JsImport.Element(importedName))
            imports[this] = when (val qualifiedReference = jsQualifier?.makeRef()) {
                null -> importStatement
                else -> JsCompositeBlock(
                    listOf(
                        importStatement,
                        jsElementAccess(declarationStableName, qualifiedReference).putIntoVariableWitName(
                            declarationStableName.toJsName()
                        )
                    )
                )
            }

        } else {
            val moduleName = JsName(sanitizeName("\$module\$$fileJsModule"), true)
            importedModules += JsImportedModule(fileJsModule, moduleName, null)
            val qualifiedReference =
                if (jsQualifier == null) moduleName.makeRef() else (listOf(moduleName) + jsQualifier).makeRef()
            imports[this] =
                jsElementAccess(declarationStableName, qualifiedReference).putIntoVariableWitName(declarationStableName.toJsName())
        }

        return getName()
    }

    private fun IrDeclarationWithName.generateRegularQualifiedImport(jsQualifier: List<JsName>?): JsName {
        val name = getJsNameOrKotlinName().identifier

        if (jsQualifier != null) {
            imports[this] = jsElementAccess(name, jsQualifier.makeRef()).putIntoVariableWitName(name.toJsName())
            return getName()
        }

        return name.toJsName(temporary = false)
    }


    private fun IrClass.fieldData(): Map<IrField, String> {
        return context.fieldDataCache.getOrPut(this) {
            val nameCnt = hashMapOf<String, Int>()

            val allClasses = DFS.topologicalOrder(listOf(this)) { node ->
                node.superTypes.mapNotNull {
                    it.safeAs<IrSimpleType>()?.classifier.safeAs<IrClassSymbol>()?.owner
                }
            }

            val result = hashMapOf<IrField, String>()

            if (minimizedMemberNames) {
                allClasses.reversed().forEach {
                    it.declarations.forEach { declaration ->
                        when {
                            declaration is IrFunction && declaration.dispatchReceiverParameter != null -> {
                                val property = (declaration as? IrSimpleFunction)?.correspondingPropertySymbol?.owner
                                if (property?.isExported(context) == true || property?.isEffectivelyExternal() == true) {
                                    context.minimizedNameGenerator.reserveName(property.getJsNameOrKotlinName().identifier)
                                }
                                if (declaration.hasStableJsName(context)) {
                                    val signature = jsFunctionSignature(declaration, context)
                                    context.minimizedNameGenerator.reserveName(signature)
                                }
                            }

                            declaration is IrProperty -> {
                                if (declaration.isExported(context)) {
                                    context.minimizedNameGenerator.reserveName(declaration.getJsNameOrKotlinName().identifier)
                                }
                            }
                        }
                    }
                }
            }

            allClasses.reversed().forEach {
                it.declarations.forEach {
                    when {
                        it is IrField -> {
                            val correspondingProperty = it.correspondingPropertySymbol?.owner
                            val hasStableName = correspondingProperty != null &&
                                    correspondingProperty.visibility.isPublicAPI &&
                                    (correspondingProperty.isExported(context) || correspondingProperty.getJsName() != null) &&
                                    correspondingProperty.isSimpleProperty
                            val safeName = when {
                               hasStableName -> (correspondingProperty ?: it).getJsNameOrKotlinName().identifier
                               minimizedMemberNames && !context.keeper.shouldKeep(it) -> context.minimizedNameGenerator.generateNextName()
                               else -> it.safeName()
                            }
                            val resultName = if (!hasStableName) {
                                val suffix = nameCnt.getOrDefault(safeName, 0) + 1
                                nameCnt[safeName] = suffix
                                safeName + "_$suffix"
                            } else safeName
                            result[it] = resultName
                        }

                        it is IrFunction && it.dispatchReceiverParameter != null -> {
                            nameCnt[jsFunctionSignature(it, context)] = 1 // avoid clashes with member functions
                        }
                    }
                }
            }

            result
        }
    }
}

private fun IrField.safeName(): String {
    return sanitizeName(name.asString()).let {
        if (it.lastOrNull()!!.isDigit()) it + "_" else it // Avoid name clashes
    }
}

private fun List<JsName>.makeRef(): JsNameRef {
    var result = this[0].makeRef()
    for (i in 1 until this.size) {
        result = JsNameRef(this[i], result)
    }
    return result
}
