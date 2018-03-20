/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.analyzer

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.resolve.BindingContext
import kotlin.reflect.KClass

abstract class Issue {
    abstract fun execute(
        irModule: IrModuleFragment,
        moduleDescriptor: ModuleDescriptor,
        bindingContext: BindingContext
    )
}

class FunctionUsageIssue(
    private var packageName: String = ""
) : Issue() {
    private var funcName: String? = null
    private val params = mutableSetOf<String>()
    private val predicates = mutableMapOf<String, (Any) -> Boolean>()
    private val paramTypes = mutableMapOf<String, KClass<Any>>()

    fun setPackage(packageName: String) {
        this.packageName = packageName
    }

    fun function(function: String) {
        funcName = function
    }

    fun param(param: String) {
        params.add(param)
    }

    fun params(params: List<String>) {
        this.params.addAll(params)
    }

    fun <T> paramPredicate(param: String, paramClass: KClass<Any>, predicate: (Any) -> Boolean) {
        predicates[param] = predicate
        paramTypes[param] = paramClass
    }

    override fun execute(irModule: IrModuleFragment, moduleDescriptor: ModuleDescriptor, bindingContext: BindingContext) {
        println("Issue")
        println(funcName)
        println(params)
        println(predicates)
        irModule.acceptVoid(MyIrVisitor())
        println("executed")
    }

    private inner class MyIrVisitor : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitFunction(declaration: IrFunction) {
            checkFunction(declaration)
            declaration.acceptChildrenVoid(this)
        }

        private fun checkFunction(declaration: IrFunction) {
            if (funcName == null) {
                return
            }
            if (declaration.descriptor.name.asString() != funcName) {
                return
            }

            if (declaration.valueParameters.map { it.name.asString() }.containsAll(params)) {
                val offset = declaration.startOffset
                println("function $funcName:$offset contains \"$params\" params")
            }
        }
    }
}

class Analyzer {
    private val issues = mutableListOf<Issue>()

    fun functionIssue(init: FunctionUsageIssue.() -> Unit) {
        val issue = FunctionUsageIssue()
        issue.init()
        issues.add(issue)
    }

    fun execute(
        irModule: IrModuleFragment,
        moduleDescriptor: ModuleDescriptor,
        bindingContext: BindingContext
    ) {
        issues.forEach { it.execute(irModule, moduleDescriptor, bindingContext) }
    }
}

fun analyzer(
    init: Analyzer.() -> Unit
): Analyzer {
    val analyzer = Analyzer()
    analyzer.init()
    return analyzer
}