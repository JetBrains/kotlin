/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.backend.common.onlyIf
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.backend.ast.*

class JsClassGenerator(private val irClass: IrClass, val context: JsGenerationContext) {

    private val className = context.getNameForSymbol(irClass.symbol)
    private val classNameRef = className.makeRef()
    private val baseClass = irClass.superTypes.firstOrNull { !it.classifierOrFail.isInterface }
    private val baseClassName = baseClass?.let { context.getNameForType(it) }
    private val classPrototypeRef = prototypeOf(classNameRef)
    private val classBlock = JsGlobalBlock()
    private val classModel = JsClassModel(className, baseClassName)

    fun generate(): JsStatement {

        if (irClass.descriptor.isExpect) return JsEmpty // TODO: fix it in Psi2Ir

        maybeGeneratePrimaryConstructor()
        val transformer = IrDeclarationToJsTransformer()

        for (declaration in irClass.declarations) {
            when (declaration) {
                is IrConstructor -> {
                    classBlock.statements += declaration.accept(transformer, context)
                    classModel.preDeclarationBlock.statements += generateInheritanceCode()
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

        translatedFunction?.let { return jsAssignment(memberRef, it.apply { name = null }).makeStmt() }

        // do not generate code like
        // interface I { foo() = "OK" }
        // interface II : I
        // II.prototype.foo = I.prototype.foo
        if (!irClass.isInterface) {
            declaration.resolveFakeOverride()?.let {
                val implClassDeclaration = it.parent as IrClass
                if (!implClassDeclaration.defaultType.isAny() && !it.isEffectivelyExternal()) {
                    val implMethodName = context.getNameForSymbol(it.symbol)
                    val implClassName = context.getNameForSymbol(implClassDeclaration.symbol)

                    val implClassPrototype = prototypeOf(implClassName.makeRef())
                    val implMemberRef = JsNameRef(implMethodName, implClassPrototype)

                    classModel.postDeclarationBlock.statements += jsAssignment(memberRef, implMemberRef).makeStmt()
                }
            }
        }

        return null
    }

    private fun maybeGenerateObjectInstance(): List<JsStatement> {
        val instanceVarName = className.objectInstanceName()
        val getInstanceFunName = "${className.ident}_getInstance"
        val jsVarNode = context.currentScope.declareName(instanceVarName)
        val varStmt = JsVars(JsVars.JsVar(jsVarNode))
        val function = generateGetInstanceFunction(jsVarNode, getInstanceFunName)
        return listOf(varStmt, function.makeStmt())
    }

    private fun generateGetInstanceFunction(instanceVar: JsName, instanceFunName: String): JsFunction {
        val functionBody = JsBlock()
        val func = JsFunction(JsFunctionScope(context.currentScope, "getInstance for ${irClass.name} object"), functionBody, "getInstance")
        func.name = context.currentScope.declareName(instanceFunName)

        functionBody.statements += JsIf(
            JsBinaryOperation(JsBinaryOperator.REF_EQ, instanceVar.makeRef(), JsPrefixOperation(JsUnaryOperator.VOID, JsIntLiteral(0))),
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
            classModel.preDeclarationBlock.statements += generateInheritanceCode()
        }
    }

    private fun generateInheritanceCode(): List<JsStatement> {
        if (baseClass == null || baseClass.isAny()) {
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

        val classKind = JsStringLiteral(
            when {
                irClass.isInterface -> "interface"
                irClass.isObject -> "object"
                else -> "class"
            }
        )
        metadataLiteral.propertyInitializers += JsPropertyInitializer(JsNameRef(Namer.METADATA_CLASS_KIND), classKind)

        metadataLiteral.propertyInitializers += generateSuperClasses()
        return jsAssignment(JsNameRef(Namer.METADATA, classNameRef), metadataLiteral).makeStmt()
    }

    private fun generateSuperClasses(): JsPropertyInitializer {
        val functionTypeOrSubtype = irClass.defaultType.isFunctionTypeOrSubtype()
        return JsPropertyInitializer(
            JsNameRef(Namer.METADATA_INTERFACES),
            JsArrayLiteral(
                irClass.superTypes.mapNotNull {
                    val symbol = it.classifierOrFail
                    // TODO: make sure that there is a test which breaks when isExternal is used here instead of isEffectivelyExternal
                    if (symbol.isInterface && !functionTypeOrSubtype && !symbol.isEffectivelyExternal) {
                        JsNameRef(context.getNameForSymbol(symbol))
                    } else null
                }
            )
        )
    }
}

private val IrClassifierSymbol.isInterface get() = (owner as? IrClass)?.isInterface == true
private val IrClassifierSymbol.isEffectivelyExternal get() = (owner as? IrDeclaration)?.isEffectivelyExternal() == true