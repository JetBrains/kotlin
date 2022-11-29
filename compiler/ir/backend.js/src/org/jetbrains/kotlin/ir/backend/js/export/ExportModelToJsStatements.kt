/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.export

import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsAstUtils
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.defineProperty
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.jsAssignment
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.prototypeOf
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.jsElementAccess
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.backend.js.utils.emptyScope
import org.jetbrains.kotlin.ir.backend.js.utils.getJsNameOrKotlinName
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.util.collectionUtils.filterIsInstanceAnd

class ExportModelToJsStatements(
    private val namer: JsStaticContext,
    private val declareNewNamespace: (String) -> String
) {
    private val namespaceToRefMap = mutableMapOf<String, JsNameRef>()

    fun generateModuleExport(
        module: ExportedModule,
        internalModuleName: JsName?,
        esModules: Boolean
    ): List<JsStatement> {
        return module.declarations.flatMap {
            generateDeclarationExport(it, internalModuleName?.makeRef(), esModules)
        }
    }

    fun generateDeclarationExport(
        declaration: ExportedDeclaration,
        namespace: JsExpression?,
        esModules: Boolean
    ): List<JsStatement> {
        return when (declaration) {
            is ExportedNamespace -> {
                require(namespace != null) { "Only namespaced namespaces are allowed" }
                val statements = mutableListOf<JsStatement>()
                val elements = declaration.name.split(".")
                var currentNamespace = ""
                var currentRef: JsExpression = namespace
                for (element in elements) {
                    val newNamespace = "$currentNamespace$$element"
                    val newNameSpaceRef = namespaceToRefMap.getOrPut(newNamespace) {
                        val varName = JsName(declareNewNamespace(newNamespace), false)
                        val namespaceRef = jsElementAccess(element, currentRef)
                        statements += JsVars(
                            JsVars.JsVar(
                                varName,
                                JsAstUtils.or(
                                    namespaceRef,
                                    jsAssignment(
                                        namespaceRef,
                                        JsObjectLiteral()
                                    )
                                )
                            )
                        )
                        JsNameRef(varName)
                    }
                    currentRef = newNameSpaceRef
                    currentNamespace = newNamespace
                }
                statements + declaration.declarations.flatMap { generateDeclarationExport(it, currentRef, esModules) }
            }

            is ExportedFunction -> {
                val name = namer.getNameForStaticDeclaration(declaration.ir)
                when {
                    namespace != null ->
                        listOf(jsAssignment(jsElementAccess(declaration.name, namespace), JsNameRef(name)).makeStmt())

                    esModules -> listOf(JsExport(name, alias = JsName(declaration.name, false)))
                    else -> emptyList()
                }
            }

            is ExportedConstructor -> emptyList()
            is ExportedConstructSignature -> emptyList()

            is ExportedProperty -> {
                require(namespace != null || esModules) { "Only namespaced properties are allowed" }
                val getter = declaration.irGetter?.let { namer.getNameForStaticDeclaration(it) }
                val setter = declaration.irSetter?.let { namer.getNameForStaticDeclaration(it) }
                if (namespace == null) {
                    val property = JsVars.JsVar(
                        JsName(declaration.name, false),
                        JsObjectLiteral(false).apply {
                            getter?.let {
                                val fieldName = when (declaration.irGetter.origin) {
                                    JsLoweredDeclarationOrigin.OBJECT_GET_INSTANCE_FUNCTION -> "getInstance"
                                    else -> "get"
                                }
                                propertyInitializers += JsPropertyInitializer(JsStringLiteral(fieldName), it.makeRef())
                            }
                            setter?.let { propertyInitializers += JsPropertyInitializer(JsStringLiteral("set"), it.makeRef()) }
                        }
                    )
                    listOf(
                        JsVars(property),
                        JsExport(property.name, JsName(declaration.name, false))
                    )
                } else {
                    listOf(defineProperty(namespace, declaration.name, getter?.makeRef(), setter?.makeRef(), namer).makeStmt())
                }
            }

            is ErrorDeclaration -> emptyList()

            is ExportedObject -> {
                require(namespace != null || esModules) { "Only namespaced properties are allowed" }
                val newNameSpace = when {
                    namespace != null -> jsElementAccess(declaration.name, namespace)
                    else ->
                        jsElementAccess(Namer.PROTOTYPE_NAME, namer.getNameForClass(declaration.ir).makeRef())
                }
                val staticsExport = declaration.nestedClasses.flatMap { generateDeclarationExport(it, newNameSpace, esModules) }

                val objectExport = when (namespace) {
                    null -> generateDeclarationExport(
                        ExportedProperty(declaration.name, ExportedType.Primitive.Any, irGetter = declaration.irGetter),
                        namespace,
                        esModules
                    )

                    else -> listOf(
                        defineProperty(
                            namespace,
                            declaration.name,
                            namer.getNameForStaticDeclaration(declaration.irGetter).makeRef(),
                            null,
                            namer
                        ).makeStmt()
                    )
                }

                objectExport + staticsExport
            }

            is ExportedRegularClass -> {
                if (declaration.isInterface) return emptyList()
                val name = namer.getNameForStaticDeclaration(declaration.ir)
                val newNameSpace = when {
                    namespace != null -> jsElementAccess(declaration.name, namespace)
                    esModules -> name.makeRef()
                    else -> prototypeOf(namer.getNameForClass(declaration.ir).makeRef(), namer)
                }
                val klassExport = when {
                    namespace != null -> jsAssignment(newNameSpace, JsNameRef(name)).makeStmt()
                    esModules -> JsExport(name, alias = JsName(declaration.name, false))
                    else -> null
                }

                // These are only used when exporting secondary constructors annotated with @JsName
                val staticFunctions = declaration.members
                    .filter { it is ExportedFunction && it.isStatic }
                    .takeIf { !declaration.ir.isInner }.orEmpty()

                val enumEntries = declaration.members.filter { it is ExportedProperty && it.isStatic }

                val innerClassesAssignments = declaration.nestedClasses
                    .filter { it.ir.isInner }
                    .map { it.generateInnerClassAssignment(declaration) }

                val staticsExport = (staticFunctions + enumEntries + declaration.nestedClasses)
                    .flatMap { generateDeclarationExport(it, newNameSpace, esModules) }

                listOfNotNull(klassExport) + staticsExport + innerClassesAssignments
            }
        }
    }

    private fun ExportedClass.generateInnerClassAssignment(outerClass: ExportedClass): JsStatement {
        val innerClassRef = namer.getNameForStaticDeclaration(ir).makeRef()
        val outerClassRef = namer.getNameForStaticDeclaration(outerClass.ir).makeRef()
        val companionObject = ir.companionObject()
        val secondaryConstructors = members.filterIsInstanceAnd<ExportedFunction> { it.isStatic }
        val bindConstructor = JsName("__bind_constructor_", false)

        val blockStatements = mutableListOf<JsStatement>(
            JsVars(JsVars.JsVar(bindConstructor, innerClassRef.bindToThis()))
        )

        if (companionObject != null) {
            val companionName = companionObject.getJsNameOrKotlinName().identifier
            blockStatements.add(
                jsAssignment(
                    JsNameRef(companionName, bindConstructor.makeRef()),
                    JsNameRef(companionName, innerClassRef),
                ).makeStmt()
            )
        }

        secondaryConstructors.forEach {
            val currentFunRef = namer.getNameForStaticDeclaration(it.ir).makeRef()
            val assignment = jsAssignment(
                JsNameRef(it.name, bindConstructor.makeRef()),
                currentFunRef.bindToThis()
            ).makeStmt()

            blockStatements.add(assignment)
        }

        blockStatements.add(JsReturn(bindConstructor.makeRef()))

        return defineProperty(
            prototypeOf(outerClassRef, namer),
            name,
            JsFunction(
                emptyScope,
                JsBlock(*blockStatements.toTypedArray()),
                "inner class '$name' getter"
            ),
            null,
            namer
        ).makeStmt()
    }

    private fun JsNameRef.bindToThis(): JsInvocation {
        return JsInvocation(
            JsNameRef("bind", this),
            JsNullLiteral(),
            JsThisRef()
        )
    }
}
