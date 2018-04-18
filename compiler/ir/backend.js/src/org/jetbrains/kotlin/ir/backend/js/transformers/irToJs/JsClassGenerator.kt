/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.backend.common.onlyIf
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.js.backend.ast.*

class JsClassGenerator(private val irClass: IrClass, val context: JsGenerationContext) {

    private val className = context.getNameForSymbol(irClass.symbol)
    private val classNameRef = className.makeRef()
    private val classPrototypeRef = prototypeOf(classNameRef)
    private val classBlock = JsBlock()

    fun generate(): JsStatement {

        maybeGeneratePrimaryConstructor()
        val transformer = IrFunctionToJsTransformer()

        for (declaration in irClass.declarations) {
            if (declaration is IrConstructor) {
                classBlock.statements += declaration.accept(transformer, context).makeStmt()
                classBlock.statements += generateInheritanceCode()
            } else if (declaration is IrFunction) {
                if (declaration.symbol.kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE &&
                    declaration.symbol.modality != Modality.ABSTRACT
                ) {
                    classBlock.statements += declaration.accept(transformer, context).let {
                        if (declaration.isStatic) {
                            it.makeStmt()
                        } else {
                            val memberName = context.getNameForSymbol(declaration.symbol)
                            val memberRef = JsNameRef(memberName, classPrototypeRef)
                            jsAssignment(memberRef, it).makeStmt()
                        }
                    }
                }
            } else if (declaration is IrClass) {
                //TODO: redesign inner classes generation
                classBlock.statements += JsClassGenerator(declaration, context).generate()
            }
        }

        classBlock.statements += generateClassMetadata()
        irClass.onlyIf({ kind == ClassKind.OBJECT }) { classBlock.statements += maybeGenerateObjectInstance() }
        return classBlock
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
        val baseClass = irClass.superClasses.first { it.kind != ClassKind.INTERFACE }
        if (baseClass.isAny) {
            return emptyList()
        }

        val baseName = context.getNameForSymbol(baseClass.owner.symbol)
        val createCall = jsAssignment(
            classPrototypeRef, JsInvocation(Namer.JS_OBJECT_CREATE_FUNCTION, prototypeOf(baseName.makeRef()))
        ).makeStmt()

        val ctorAssign = jsAssignment(JsNameRef(Namer.CONSTRUCTOR_NAME, classPrototypeRef), classNameRef).makeStmt()

        return listOf(createCall, ctorAssign)
    }

    private fun generateClassMetadata(): JsStatement {
        val metadataLiteral = JsObjectLiteral(true)
        val simpleName = irClass.symbol.name

        if (!simpleName.isSpecial) {
            val simpleNameProp = JsPropertyInitializer(JsNameRef(Namer.METADATA_SIMPLE_NAME), JsStringLiteral(simpleName.identifier))
            metadataLiteral.propertyInitializers += simpleNameProp
        }

        metadataLiteral.propertyInitializers += generateSuperClasses()
        return jsAssignment(JsNameRef(Namer.METADATA, classNameRef), metadataLiteral).makeStmt()
    }

    private fun generateSuperClasses(): JsPropertyInitializer = JsPropertyInitializer(
        JsNameRef(Namer.METADATA_INTERFACES),
        JsArrayLiteral(irClass.superClasses.filter { it.kind == ClassKind.INTERFACE }.map { JsNameRef(context.getNameForSymbol(it.owner.symbol)) })
    )

}
