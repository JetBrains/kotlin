/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.analyzer

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.resolve.BindingContext
import kotlin.reflect.KClass

class FunctionDeclarationIssue(
    private var funcName: String? = null,
    private var packageName: String? = null
) : Issue() {
    private val params = mutableSetOf<String>()
    private val paramsWithType = mutableMapOf<String, KClass<*>>()

    fun setPackage(packageName: String) {
        this.packageName = packageName
    }

    fun function(function: String) {
        funcName = function
    }

    fun param(param: String) {
        params.add(param)
    }

    fun <T : Any> param(param: String, clazz: KClass<T>) {
        paramsWithType[param] = clazz
    }

    fun params(params: List<String>) {
        this.params.addAll(params)
    }


    override fun execute(irModule: IrModuleFragment, moduleDescriptor: ModuleDescriptor, bindingContext: BindingContext) {
        println("Issue")
        println(funcName)
        println(params)
        irModule.acceptVoid(MyIrVisitor())
        println("executed")
    }

    private inner class MyIrVisitor : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitPackageFragment(declaration: IrPackageFragment) {
            if (packageName != null && declaration.fqName.asString() != packageName) {
                return
            }
            declaration.acceptChildrenVoid(this)
        }

        override fun visitFunction(declaration: IrFunction) {
            checkFunction(declaration)
            declaration.acceptChildrenVoid(this)
        }

        private fun checkFunction(declaration: IrFunction) {
            // TODO: add try catch on

            if (funcName == null) {
                return
            }
            if (declaration.descriptor.name.asString() != funcName) {
                return
            }

            val offset = declaration.startOffset
            if (params.isNotEmpty() && declaration.valueParameters.map { it.name.identifier }.containsAll(params)) {
                println("function $funcName:$offset contains \"$params\" parameters")
            }

            for (paramDeclaration in declaration.valueParameters) {
                val paramName = paramDeclaration.name.identifier

                if (paramName in paramsWithType) {
                    val expectedType = classNameFromKClass(paramsWithType[paramName]!!)
                    val actualType = classNameFromDescriptor(paramDeclaration)
                    if (actualType != null && actualType == expectedType) {
                        println("function $funcName:$offset contains \"$paramName:$actualType\" parameter")
                    }
                }
            }
        }
    }

    private fun classNameFromDescriptor(declaration: IrValueParameter): String? = declaration.descriptor.toString().split(" ").getOrNull(2)

    private fun classNameFromKClass(clazz: KClass<*>): String? = clazz.toString().split(" ").getOrNull(1)
}