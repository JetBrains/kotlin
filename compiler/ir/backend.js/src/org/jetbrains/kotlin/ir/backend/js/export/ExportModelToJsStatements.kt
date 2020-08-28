/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.export

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsAstUtils
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.defineProperty
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.jsAssignment
import org.jetbrains.kotlin.ir.backend.js.utils.IrNamer
import org.jetbrains.kotlin.js.backend.ast.*


class ExportModelToJsStatements(
    private val internalModuleName: JsName,
    private val namer: IrNamer,
    private val declareNewNamespace: (String) -> String
) {
    private val namespaceToRefMap = mutableMapOf<String, JsNameRef>()

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
                        val varName = declareNewNamespace(newNamespace)
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
                        JsNameRef(namer.getNameForStaticDeclaration(declaration.ir))
                    ).makeStmt()
                )
            }

            is ExportedConstructor -> emptyList()

            is ExportedProperty -> {
                val getter = declaration.irGetter?.let { JsNameRef(namer.getNameForStaticDeclaration(it)) }
                val setter = declaration.irSetter?.let { JsNameRef(namer.getNameForStaticDeclaration(it)) }
                listOf(defineProperty(namespace, declaration.name, getter, setter).makeStmt())
            }

            is ErrorDeclaration -> emptyList()

            is ExportedClass -> {
                if (declaration.isInterface) return emptyList()
                val newNameSpace = JsNameRef(declaration.name, namespace)
                val klassExport = jsAssignment(
                    newNameSpace,
                    JsNameRef(
                        namer.getNameForStaticDeclaration(
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
