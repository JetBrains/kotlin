/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.backend.common.onlyIf
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.backend.js.utils.isAny
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.isReal
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
import org.jetbrains.kotlin.js.backend.ast.*

class JsClassGenerator(private val irClass: IrClass, val context: JsGenerationContext) {

    private val className = context.getNameForSymbol(irClass.symbol)
    private val classNameRef = className.makeRef()
    private val baseClass = irClass.superClasses.firstOrNull { !it.owner.isInterface }
    private val baseClassName = baseClass?.let { context.getNameForSymbol(it) }
    private val classPrototypeRef = prototypeOf(classNameRef)
    private val classBlock = JsBlock()
    private val classModel = JsClassModel(className, baseClassName)

    fun generate(): JsStatement {

        maybeGeneratePrimaryConstructor()
        val transformer = IrDeclarationToJsTransformer()

        for (declaration in irClass.declarations) {
            when (declaration) {
                is IrConstructor -> {
                    classBlock.statements += declaration.accept(transformer, context)
                    classBlock.statements += generateInheritanceCode()
                }
                is IrSimpleFunction -> {
                    generateMemberFunction(declaration)?.let { classBlock.statements += it }
                }
                is IrClass -> {
                    classBlock.statements += JsClassGenerator(declaration, context).generate()
                }
                is IrVariable -> {
                    classBlock.statements += declaration.accept(transformer, context)
                }
                else -> {
                }
            }
        }

        classBlock.statements += generateClassMetadata()
        irClass.onlyIf({ kind == ClassKind.OBJECT }) { classBlock.statements += maybeGenerateObjectInstance() }
        context.staticContext.classModels[className] = classModel
        return classBlock
    }

    private fun generateMemberFunction(declaration: IrSimpleFunction): JsStatement? {

        val translatedFunction = declaration.run { if (isReal) accept(IrFunctionToJsTransformer(), context) else null }
        if (declaration.isStatic) {
            return translatedFunction!!.makeStmt()
        }

        val memberName = context.getNameForSymbol(declaration.symbol)
        val memberRef = JsNameRef(memberName, classPrototypeRef)

        translatedFunction?.let { return jsAssignment(memberRef, it).makeStmt() }

        // do not generate code like
        // interface I { foo() = "OK" }
        // interface II : I
        // II.prototype.foo = I.prototype.foo
        if (!irClass.isInterface) {
            declaration.resolveFakeOverride()?.let {
                val implClassDesc = it.descriptor.containingDeclaration as ClassDescriptor
                if (!KotlinBuiltIns.isAny(implClassDesc)) {
                    val implMethodName = context.getNameForSymbol(it.symbol)
                    val implClassName = context.getNameForSymbol(IrClassSymbolImpl(implClassDesc))

                    val implClassPrototype = prototypeOf(implClassName.makeRef())
                    val implMemberRef = JsNameRef(implMethodName, implClassPrototype)

                    classModel.postDeclarationBlock.statements += jsAssignment(memberRef, implMemberRef).makeStmt()
                }
            }
        }

        return null
    }

    private fun maybeGenerateObjectInstance(): List<JsStatement> {
        val instanceVarName = "${className.ident}_instance"
        val getInstanceFunName = "${className.ident}_getInstance"
        val jsVarNode = context.currentScope.declareName(instanceVarName)
        val varStmt = JsVars(JsVars.JsVar(jsVarNode, JsNullLiteral()))
        val function = generateGetInstanceFunction(jsVarNode, getInstanceFunName)
        return listOf(varStmt, function.makeStmt())
    }

    private fun generateGetInstanceFunction(instanceVar: JsName, instanceFunName: String): JsFunction {
        val functionBody = JsBlock()
        val func = JsFunction(JsFunctionScope(context.currentScope, "getInstance for ${irClass.name} object"), functionBody, "getInstance")
        func.name = context.currentScope.declareName(instanceFunName)

        functionBody.statements += JsIf(
            JsBinaryOperation(JsBinaryOperator.REF_EQ, instanceVar.makeRef(), JsNullLiteral()),
            jsAssignment(instanceVar.makeRef(), JsNew(classNameRef)).makeStmt()
        )
        functionBody.statements += JsReturn(instanceVar.makeRef())

        return func
    }

    private fun maybeGeneratePrimaryConstructor() {
        if (!irClass.declarations.any { it is IrConstructor }) {
            val func = JsFunction(JsFunctionScope(context.currentScope, "Ctor for ${irClass.name}"), JsBlock(), "Ctor for ${irClass.name}")
            func.name = className
            classBlock.statements += func.makeStmt()
            classBlock.statements += generateInheritanceCode()
        }
    }

    private fun generateInheritanceCode(): List<JsStatement> {
        if (baseClass == null || baseClass.isAny) {
            return emptyList()
        }

        val createCall = jsAssignment(
            classPrototypeRef, JsInvocation(Namer.JS_OBJECT_CREATE_FUNCTION, prototypeOf(baseClassName!!.makeRef()))
        ).makeStmt()

        val ctorAssign = jsAssignment(JsNameRef(Namer.CONSTRUCTOR_NAME, classPrototypeRef), classNameRef).makeStmt()

        return listOf(createCall, ctorAssign)
    }

    private fun generateClassMetadata(): JsStatement {
        val metadataLiteral = JsObjectLiteral(true)
        val simpleName = irClass.name

        if (!simpleName.isSpecial) {
            val simpleNameProp = JsPropertyInitializer(JsNameRef(Namer.METADATA_SIMPLE_NAME), JsStringLiteral(simpleName.identifier))
            metadataLiteral.propertyInitializers += simpleNameProp
        }

        metadataLiteral.propertyInitializers += generateSuperClasses()
        return jsAssignment(JsNameRef(Namer.METADATA, classNameRef), metadataLiteral).makeStmt()
    }

    private fun generateSuperClasses(): JsPropertyInitializer = JsPropertyInitializer(
        JsNameRef(Namer.METADATA_INTERFACES),
        JsArrayLiteral(irClass.superClasses.filter { it.owner.isInterface }.map { JsNameRef(context.getNameForSymbol(it.owner.symbol)) })
    )

}
