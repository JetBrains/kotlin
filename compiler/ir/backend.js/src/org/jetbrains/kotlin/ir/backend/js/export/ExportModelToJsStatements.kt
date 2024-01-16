/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.export

import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.lower.isEs6ConstructorReplacement
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsAstUtils
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.defineProperty
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.jsAssignment
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.prototypeOf
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.jsElementAccess
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.backend.js.utils.emptyScope
import org.jetbrains.kotlin.ir.backend.js.utils.getJsNameOrKotlinName
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.utils.filterIsInstanceAnd

class ExportModelToJsStatements(
    private val staticContext: JsStaticContext,
    private val es6mode: Boolean,
    private val declareNewNamespace: (String) -> String,
) {
    private val namespaceToRefMap = hashMapOf<String, JsNameRef>()

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
        esModules: Boolean,
        parentClass: IrClass? = null
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
                val name = staticContext.getNameForStaticDeclaration(declaration.ir)
                when {
                    namespace != null ->
                        listOf(jsAssignment(jsElementAccess(declaration.name, namespace), JsNameRef(name)).makeStmt())

                    esModules -> listOf(JsExport(name.makeRef(), alias = JsName(declaration.name, false)))
                    else -> emptyList()
                }
            }

            is ExportedConstructor -> emptyList()
            is ExportedConstructSignature -> emptyList()

            is ExportedProperty -> {
                require(namespace != null || esModules) { "Only namespaced properties are allowed" }
                when (namespace) {
                    null -> {
                        val property = declaration.generateTopLevelGetters()
                        listOf(JsVars(property), JsExport(property.name.makeRef(), JsName(declaration.name, false)))
                    }
                    else -> {
                        val getter = declaration.irGetter?.let { staticContext.getNameForStaticDeclaration(it) }
                        val setter = declaration.irSetter?.let { staticContext.getNameForStaticDeclaration(it) }
                        listOf(
                            defineProperty(
                                namespace,
                                declaration.name,
                                getter?.makeRef(),
                                setter?.makeRef(),
                                staticContext
                            ).makeStmt()
                        )
                    }
                }
            }

            is ErrorDeclaration -> emptyList()

            is ExportedObject -> {
                require(namespace != null || esModules) { "Only namespaced properties are allowed" }
                val (name, objectClassInitialization) = declaration.getNameAndInitialization()
                val newNameSpace = jsElementAccess(Namer.PROTOTYPE_NAME, name.makeRef())
                val staticsExport =
                    declaration.nestedClasses.flatMap { generateDeclarationExport(it, newNameSpace, esModules, declaration.ir) }

                val objectExport = when {
                    es6mode || namespace == null -> generateDeclarationExport(
                        ExportedProperty(
                            declaration.name,
                            ExportedType.Primitive.Any,
                            isStatic = parentClass?.isObject != true,
                            irGetter = declaration.irGetter,
                            isMember = parentClass != null
                        ),
                        namespace,
                        esModules,
                        parentClass
                    )
                    else -> listOf(
                        defineProperty(
                            namespace,
                            declaration.name,
                            staticContext.getNameForStaticDeclaration(
                                declaration.irGetter
                                    ?: error("Expect to have an object getter in its export model, but ${declaration.ir.fqNameWhenAvailable ?: declaration.name} doesn't have it")
                            ).makeRef(),
                            null,
                            staticContext
                        ).makeStmt()
                    )
                }

                listOfNotNull(objectClassInitialization.takeIf { staticsExport.isNotEmpty() }) + objectExport + staticsExport
            }

            is ExportedRegularClass -> {
                if (declaration.isInterface) return emptyList()
                val (name, classInitialization) = declaration.getNameAndInitialization()
                val newNameSpace = when {
                    namespace != null -> jsElementAccess(declaration.name, namespace)
                    esModules -> name.makeRef()
                    else -> prototypeOf(name.makeRef(), staticContext)
                }
                val klassExport = when {
                    namespace != null -> jsAssignment(newNameSpace, name.makeRef()).makeStmt()
                    esModules -> JsExport(name.makeRef(), alias = JsName(declaration.name, false))
                    else -> null
                }

                // These are only used when exporting secondary constructors annotated with @JsName
                val staticFunctions = declaration.members
                    .filter { it is ExportedFunction && it.isStatic && !it.ir.isEs6ConstructorReplacement }
                    .takeIf { !declaration.ir.isInner }.orEmpty()

                val enumEntries = declaration.members.filter { it is ExportedProperty && it.isStatic }

                val innerClassesAssignments = declaration.nestedClasses
                    .filter { it.ir.isInner }
                    .map { it.generateInnerClassAssignment(name) }

                val staticsExport = (staticFunctions + enumEntries + declaration.nestedClasses)
                    .flatMap { generateDeclarationExport(it, newNameSpace, esModules, declaration.ir) }

                listOfNotNull(classInitialization, klassExport) + staticsExport + innerClassesAssignments
            }
        }
    }

    private fun ExportedProperty.generateTopLevelGetters(): JsVars.JsVar {
        val getter = irGetter?.let { staticContext.getNameForStaticDeclaration(it) }
        val setter = irSetter?.let { staticContext.getNameForStaticDeclaration(it) }

        return JsVars.JsVar(
            JsName(name, false),
            JsObjectLiteral(false).apply {
                getter?.let {
                    val fieldName = when (irGetter?.origin) {
                        JsLoweredDeclarationOrigin.OBJECT_GET_INSTANCE_FUNCTION -> "getInstance"
                        else -> "get"
                    }
                    propertyInitializers += JsPropertyInitializer(JsStringLiteral(fieldName), it.makeRef())
                }
                setter?.let { propertyInitializers += JsPropertyInitializer(JsStringLiteral("set"), it.makeRef()) }
            }
        )
    }

    private fun ExportedClass.generateInnerClassAssignment(outerClassName: JsName): JsStatement {
        val innerClassRef = ir.getClassRef(staticContext)
        val outerClassRef = outerClassName.makeRef()
        val companionObject = ir.companionObject()
        val secondaryConstructors = members.filterIsInstanceAnd<ExportedFunction> { it.isStatic }
        val bindConstructor = JsName("__bind_constructor_", false)

        val blockStatements = mutableListOf<JsStatement>(
            JsVars(JsVars.JsVar(bindConstructor, innerClassRef.bindToThis(innerClassRef)))
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
            val currentFunRef = if (it.ir.isEs6ConstructorReplacement) {
                JsNameRef(staticContext.getNameForMemberFunction(it.ir), innerClassRef)
            } else {
                staticContext.getNameForStaticDeclaration(it.ir).makeRef()
            }

            val assignment = jsAssignment(
                JsNameRef(it.name, bindConstructor.makeRef()),
                currentFunRef.bindToThis(innerClassRef)
            ).makeStmt()

            blockStatements.add(assignment)
        }

        blockStatements.add(JsReturn(bindConstructor.makeRef()))
        val innerClassGetter = JsFunction(emptyScope, JsBlock(*blockStatements.toTypedArray()), "inner class '$name' getter")

        return defineProperty(
            prototypeOf(outerClassRef, staticContext),
            name,
            innerClassGetter,
            null,
            staticContext
        ).makeStmt()
    }

    private fun ExportedClass.getNameAndInitialization(): Pair<JsName, JsStatement?> {
        return when (val classRef = ir.getClassRef(staticContext)) {
            is JsNameRef -> classRef.name!! to null
            else -> {
                val stableName = JsName(name, true)
                stableName to JsVars(JsVars.JsVar(stableName, classRef))
            }
        }
    }


    private fun JsExpression.bindToThis(bindTo: JsExpression): JsInvocation {
        return JsInvocation(JsNameRef("bind", this), bindTo, JsThisRef())
    }
}
