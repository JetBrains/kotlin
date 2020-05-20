/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.export

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsAstUtils
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.defineProperty
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.jsAssignment
import org.jetbrains.kotlin.ir.backend.js.utils.NameTable
import org.jetbrains.kotlin.ir.backend.js.utils.NameTables
import org.jetbrains.kotlin.js.backend.ast.*


class ExportModelToJsStatements(
    private val internalModuleName: JsName,
    private val nameTables: NameTables
) {
    private val namespaceToRefMap = mutableMapOf<String, JsNameRef>()
    private val globalNames = NameTable<String>(nameTables.globalNames)

    fun generateModuleExport(module: ExportedModule): List<JsStatement> {
        return module.declarations.flatMap { generateDeclarationExport(it, JsNameRef(internalModuleName)) }
    }

    private fun generateDeclarationExport(declaration: ExportedDeclaration, namespace: JsNameRef): List<JsStatement> {
        return when (declaration) {
            is ExportedNamespace -> {
                val statements = mutableListOf<JsStatement>()
                val elements = declaration.name.split(".")
                var currentNamespace = ""
                var currentRef = namespace
                for (element in elements) {
                    val newNamespace = "$currentNamespace$$element"
                    val newNameSpaceRef = namespaceToRefMap.getOrPut(newNamespace) {
                        val varName = globalNames.declareFreshName(newNamespace, newNamespace)
                        val varRef = JsNameRef(varName)
                        val namespaceRef = JsNameRef(element, currentRef)
                        statements += JsVars(
                            JsVars.JsVar(JsName(varName),
                                         JsAstUtils.or(
                                             namespaceRef,
                                             jsAssignment(
                                                 namespaceRef,
                                                 JsObjectLiteral()
                                             )
                                         )
                            )
                        )
                        varRef
                    }
                    currentRef = newNameSpaceRef
                    currentNamespace = newNamespace
                }
                statements + declaration.declarations.flatMap { generateDeclarationExport(it, currentRef) }
            }

            is ExportedFunction -> {
                listOf(
                    jsAssignment(
                        JsNameRef(declaration.name, namespace),
                        JsNameRef(nameTables.getNameForStaticDeclaration(declaration.ir))
                    ).makeStmt()
                )
            }

            is ExportedConstructor -> emptyList()

            is ExportedProperty -> {
                val getter = declaration.ir.getter?.let { JsNameRef(nameTables.getNameForStaticDeclaration(it)) }
                val setter = declaration.ir.setter?.let { JsNameRef(nameTables.getNameForStaticDeclaration(it)) }
                listOf(defineProperty(namespace, declaration.name, getter, setter).makeStmt())
            }

            is ErrorDeclaration -> emptyList()

            is ExportedClass -> {
                if (declaration.isInterface) return emptyList()
                val newNameSpace = JsNameRef(declaration.name, namespace)
                val klassExport = jsAssignment(
                    newNameSpace,
                    JsNameRef(
                        nameTables.getNameForStaticDeclaration(
                            declaration.ir
                        )
                    )
                ).makeStmt()

                val staticFunctions = declaration.members.filter { it is ExportedFunction && it.isStatic }

                val staticsExport = (staticFunctions + declaration.nestedClasses)
                    .flatMap { generateDeclarationExport(it, newNameSpace) }

                listOf(klassExport) + staticsExport
            }
        }
    }
}
