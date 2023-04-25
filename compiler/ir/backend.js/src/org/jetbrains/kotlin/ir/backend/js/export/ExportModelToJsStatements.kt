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
                when {
                    namespace == null -> {
                        val property = declaration.generateTopLevelGetters()
                        listOf(JsVars(property), JsExport(property.name.makeRef(), JsName(declaration.name, false)))
                    }
                    es6mode && declaration.isMember -> {
                        val jsClass = parentClass?.getCorrespondingJsClass() ?: error("Expect to have parentClass at this point")
                        jsClass.members += declaration.generateClassMembers()
                        listOf(JsEmpty)
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
                val newNameSpace = when {
                    namespace != null -> jsElementAccess(declaration.name, namespace)
                    else ->
                        jsElementAccess(Namer.PROTOTYPE_NAME, staticContext.getNameForClass(declaration.ir).makeRef())
                }
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
                            staticContext.getNameForStaticDeclaration(declaration.irGetter).makeRef(),
                            null,
                            staticContext
                        ).makeStmt()
                    )
                }

                objectExport + staticsExport
            }

            is ExportedRegularClass -> {
                if (declaration.isInterface) return emptyList()
                val name = staticContext.getNameForStaticDeclaration(declaration.ir)
                val newNameSpace = when {
                    namespace != null -> jsElementAccess(declaration.name, namespace)
                    esModules -> name.makeRef()
                    else -> prototypeOf(staticContext.getNameForClass(declaration.ir).makeRef(), staticContext)
                }
                val klassExport = when {
                    namespace != null -> jsAssignment(newNameSpace, JsNameRef(name)).makeStmt()
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
                    .map { it.generateInnerClassAssignment(declaration) }

                val staticsExport = (staticFunctions + enumEntries + declaration.nestedClasses)
                    .flatMap { generateDeclarationExport(it, newNameSpace, esModules, declaration.ir) }

                listOfNotNull(klassExport) + staticsExport + innerClassesAssignments
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

    private fun ExportedProperty.generateClassMembers(): List<JsFunction> {
        val getter = irGetter?.let { staticContext.getNameForStaticDeclaration(it) }
        val setter = irSetter?.let { staticContext.getNameForStaticDeclaration(it) }

        return buildList {
            if (getter != null) {
                add(JsFunction(emptyScope, "").also {
                    it.name = JsName(name, false)
                    if (isStatic) {
                        it.modifiers.add(JsFunction.Modifier.STATIC)
                    }
                    it.modifiers.add(JsFunction.Modifier.GET)
                    it.body = JsBlock().apply {
                        statements.add(JsReturn(JsInvocation(getter.makeRef())))
                    }
                })
            }
            if (setter != null) {
                add(JsFunction(emptyScope, "").also {
                    val value = JsName("value", true)
                    it.name = JsName(name, false)
                    it.parameters.add(JsParameter(value))
                    if (isStatic) {
                        it.modifiers.add(JsFunction.Modifier.STATIC)
                    }
                    it.modifiers.add(JsFunction.Modifier.SET)
                    it.body = JsBlock().apply {
                        statements.add(JsExpressionStatement(JsInvocation(setter.makeRef(), value.makeRef())))
                    }
                })
            }
        }
    }

    private fun ExportedClass.generateInnerClassAssignment(outerClass: ExportedClass): JsStatement {
        val innerClassRef = staticContext.getNameForStaticDeclaration(ir).makeRef()
        val outerClassRef = staticContext.getNameForStaticDeclaration(outerClass.ir).makeRef()
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

        return if (es6mode) {
            outerClass.ir.getCorrespondingJsClass().members += innerClassGetter.also {
                it.name = JsName(name, false)
                it.modifiers.add(JsFunction.Modifier.GET)
            }
            JsEmpty
        } else {
            defineProperty(
                prototypeOf(outerClassRef, staticContext),
                name,
                innerClassGetter,
                null,
                staticContext
            ).makeStmt()
        }
    }

    private fun IrClass.getCorrespondingJsClass(): JsClass {
        val jsClassModel = staticContext.classModels[symbol] ?: error("Class with name '$name' was not found")
        return (jsClassModel.preDeclarationBlock.statements.first() as? JsExpressionStatement)?.expression as? JsClass
            ?: error("Expect to have JsClass as a first statement inside JsIrClassModel")
    }

    private fun JsNameRef.bindToThis(bindTo: JsExpression): JsInvocation {
        return JsInvocation(JsNameRef("bind", this), bindTo, JsThisRef())
    }
}
