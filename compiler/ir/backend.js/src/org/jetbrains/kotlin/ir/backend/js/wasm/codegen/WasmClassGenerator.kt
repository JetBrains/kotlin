/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.wasm.codegen

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIrClassModel
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.utils.addIfNotNull

class WasmClassGenerator(private val irClass: IrClass, val context: WasmStaticContext) {
    private val className = context.getNameForClass(irClass)
    private val classNameRef = className.makeRef()
    private val baseClass: IrType? = irClass.superTypes.firstOrNull { !it.classifierOrFail.isInterface }
    private val classPrototypeRef = prototypeOf(classNameRef)
    private val classBlock = JsGlobalBlock()
    private val classModel = JsIrClassModel(irClass)

    fun generate(): JsStatement {
        classBlock.statements += JsDocComment(mapOf("class" to irClass.fqNameWhenAvailable.toString())).makeStmt()
        assert(!irClass.descriptor.isExpect)

        val transformer = IrDeclarationToWasmTransformer()

        for (declaration in irClass.declarations) {
            when (declaration) {
                is IrConstructor -> {
                    classBlock.statements += declaration.accept(transformer, context)
                }
                is IrSimpleFunction -> {
                    classBlock.statements.addIfNotNull(generateMemberFunction(declaration))
                }
                is IrClass -> {
                    classBlock.statements += WasmClassGenerator(declaration, context).generate()
                }
                is IrField -> {
                }
                else -> {
                    // error("Unexpected declaration in class: $declaration")
                }
            }
        }

        context.classModels[irClass.symbol] = classModel
        return classBlock
    }

    private fun generateMemberFunction(declaration: IrSimpleFunction): JsStatement? {

        val translatedFunction = declaration.run { if (isReal) accept(IrFunctionToWasmTransformer(), context) else null } as JsFunction?
        assert(!declaration.isStaticMethodOfClass)

        val memberName = context.getNameForMemberFunction(declaration.realOverrideTarget)
        val memberRef = JsNameRef(memberName, classPrototypeRef)

        translatedFunction?.let { return jsAssignment(memberRef, it.apply { name = null }).makeStmt() }

        // do not generate code like
        // interface I { foo() = "OK" }
        // interface II : I
        // II.prototype.foo = I.prototype.foo
        if (!irClass.isInterface) {
            declaration.realOverrideTarget.let { it ->
                val implClassDeclaration = it.parent as IrClass

                if (!implClassDeclaration.defaultType.isAny() && !it.isEffectivelyExternal()) {
                    val implMethodName = context.getNameForMemberFunction(it)
                    val implClassName = context.getNameForClass(implClassDeclaration)

                    val implClassPrototype = prototypeOf(implClassName.makeRef())
                    val implMemberRef = JsNameRef(implMethodName, implClassPrototype)

                    classModel.postDeclarationBlock.statements += jsAssignment(memberRef, implMemberRef).makeStmt()
                }
            }
        }

        return null
    }
}

private val IrClassifierSymbol.isInterface get() = (owner as? IrClass)?.isInterface == true
