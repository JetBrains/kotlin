/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.utils.IrNamer
import org.jetbrains.kotlin.ir.backend.js.utils.sanitizeName
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.js.backend.ast.JsName
import org.jetbrains.kotlin.name.Name

interface CrossModuleReferenceInfo {
    fun exports(module: IrModuleFragment): List<String>

    fun withReferenceTracking(namer: IrNamer, excludedModules: Iterable<IrModuleFragment>): IrNamerWithImports
}

interface IrNamerWithImports : IrNamer {
    fun imports(): List<Pair<IrModuleFragment, List<String>>>
}

object EmptyCrossModuleReferenceInfo : CrossModuleReferenceInfo {
    override fun exports(module: IrModuleFragment): List<String> = emptyList()

    override fun withReferenceTracking(namer: IrNamer, excludedModules: Iterable<IrModuleFragment>): IrNamerWithImports =
        object : IrNamerWithImports, IrNamer by namer {
            override fun imports() = emptyList<Pair<IrModuleFragment, List<String>>>()
        }
}

private class CrossModuleReferenceInfoImpl(val topLevelDeclarationToModule: Map<IrDeclaration, IrModuleFragment>) :
    CrossModuleReferenceInfo {

    private val exportedNames: MutableMap<IrModuleFragment, MutableSet<String>> = mutableMapOf()

    override fun exports(module: IrModuleFragment): List<String> {
        return exportedNames[module]?.toList() ?: emptyList()
    }

    override fun withReferenceTracking(namer: IrNamer, excludedModules: Iterable<IrModuleFragment>): IrNamerWithImports {
        val excludedModulesSet = excludedModules.toSet()

        return object : IrNamerWithImports, IrNamer by namer {

            override fun imports(): List<Pair<IrModuleFragment, List<String>>> {
                return importedNames.entries.map { (module, names) -> module to names.toList() }
            }

            private val importedNames: MutableMap<IrModuleFragment, MutableSet<String>> = mutableMapOf()

            private fun JsName.track(d: IrDeclaration): JsName {
                topLevelDeclarationToModule[d]?.let { module ->
                    if (module !in excludedModulesSet) {
                        importedNames.getOrPut(module) { mutableSetOf() } += this.ident
                        exportedNames.getOrPut(module) { mutableSetOf() } += this.ident
                    }
                }

                return this
            }

            override fun getNameForConstructor(constructor: IrConstructor): JsName {
                return namer.getNameForConstructor(constructor).track(constructor.constructedClass)
            }

            override fun getNameForField(field: IrField): JsName {
                return namer.getNameForField(field).track(field)
            }

            override fun getNameForClass(klass: IrClass): JsName {
                return namer.getNameForClass(klass).track(klass)
            }

            override fun getNameForStaticFunction(function: IrSimpleFunction): JsName {
                return namer.getNameForStaticFunction(function).track(function)
            }

            override fun getNameForStaticDeclaration(declaration: IrDeclarationWithName): JsName {
                return namer.getNameForStaticDeclaration(declaration).track(declaration)
            }

            override fun getNameForProperty(property: IrProperty): JsName {
                return namer.getNameForProperty(property).track(property)
            }
        }
    }

}

fun buildCrossModuleReferenceInfo(modules: Iterable<IrModuleFragment>): CrossModuleReferenceInfo {
    val map = mutableMapOf<IrDeclaration, IrModuleFragment>()

    modules.forEach { module ->
        module.files.forEach { file ->
            file.declarations.forEach { declaration ->
                map[declaration] = module
            }
        }
    }

    return CrossModuleReferenceInfoImpl(map)
}

fun breakCrossModuleFieldAccess(
    context: JsIrBackendContext,
    modules: Iterable<IrModuleFragment>
) {
    val fieldToGetter = mutableMapOf<IrField, IrSimpleFunction>()

    fun IrField.getter(): IrSimpleFunction {
        return fieldToGetter.getOrPut(this) {
            val fieldName = name
            val getter = context.irFactory.buildFun {
                name = Name.identifier("get-$fieldName")
                returnType = type
            }
            getter.body = factory.createBlockBody(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, listOf(
                    IrReturnImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, getter.symbol,
                        IrGetFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbol, type)
                    )
                )
            )
            getter.parent = parent
            (parent as IrDeclarationContainer).declarations += getter

            getter
        }
    }

    val fieldToSetter = mutableMapOf<IrField, IrSimpleFunction>()

    fun IrField.setter(): IrSimpleFunction {
        return fieldToSetter.getOrPut(this) {
            val fieldName = name
            val setter = context.irFactory.buildFun {
                name = Name.identifier("set-$fieldName")
                returnType = context.irBuiltIns.unitType
            }

            val param = setter.addValueParameter("value", type)

            setter.body = factory.createBlockBody(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, listOf(
                    IrSetFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbol, type).apply {
                        value = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, param.symbol)
                    }
                )
            )
            setter.parent = parent
            (parent as IrDeclarationContainer).declarations += setter

            setter
        }
    }

    modules.reversed().forEach { module ->

        val moduleFields = module.files.flatMap { it.declarations.filterIsInstance<IrField>() }.toSet()

        fun IrField.transformAccess(fn: IrField.() -> IrCall): IrCall? {
            if (parent !is IrPackageFragment || isEffectivelyExternal() || this in moduleFields) return null
            return fn()
        }

        module.transformChildrenVoid(object : IrElementTransformerVoid() {

            override fun visitGetField(expression: IrGetField): IrExpression {
                expression.transformChildrenVoid(this)

                return expression.symbol.owner.transformAccess {
                    val getter = getter()
                    IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, getter.returnType, getter.symbol, getter.typeParameters.size, getter.valueParameters.size)
                } ?: expression
            }

            override fun visitSetField(expression: IrSetField): IrExpression {
                expression.transformChildrenVoid(this)

                return expression.symbol.owner.transformAccess {
                    val setter = setter()
                    IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, setter.returnType, setter.symbol, setter.typeParameters.size, setter.valueParameters.size).apply {
                        putValueArgument(0, expression.value)
                    }
                } ?: expression
            }
        })
    }
}

val IrModuleFragment.safeName: String
    get() {
        var result = name.asString()

        if (result.startsWith('<')) result = result.substring(1)
        if (result.endsWith('>')) result = result.substring(0, result.length - 1)

        return sanitizeName("kotlin-$result")
    }
