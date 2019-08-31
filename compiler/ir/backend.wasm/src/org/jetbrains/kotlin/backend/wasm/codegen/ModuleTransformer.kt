/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.common.ir.isOverridableOrOverrides
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.WasmCompilerResult
import org.jetbrains.kotlin.backend.wasm.ast.*
import org.jetbrains.kotlin.backend.wasm.utils.hasExcludedFromCodegenAnnotation
import org.jetbrains.kotlin.backend.wasm.utils.hasSkipRTTIAnnotation
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.jsAssignment
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.js.backend.ast.JsArrayLiteral
import org.jetbrains.kotlin.js.backend.ast.JsBlock
import org.jetbrains.kotlin.js.backend.ast.JsNameRef
import org.jetbrains.kotlin.js.backend.ast.JsStringLiteral
import org.jetbrains.kotlin.utils.DFS

private val IrClass.superBroadClasses: List<IrClass>
    get() = superTypes.map { it.classifierOrFail.owner as IrClass }

fun IrClass.allInterfaces(): List<IrClass> {
    val shallowSuperClasses = superBroadClasses
    return shallowSuperClasses.filter { it.isInterface } + shallowSuperClasses.flatMap { it.allInterfaces() }
}


fun List<IrFunction>.filterVirtualFunctions(): List<IrSimpleFunction> =
    asSequence()
        .filterIsInstance<IrSimpleFunction>()
        .filter { it.dispatchReceiverParameter != null }
        .map { it.realOverrideTarget }
        .filter { it.isOverridableOrOverrides }
        .distinct()
        .toList()

private fun IrDeclaration.collectFunctions(): List<IrFunction> =
    when {
        hasExcludedFromCodegenAnnotation() -> emptyList()
        this is IrFunction -> listOf(this)
        this is IrClass -> this.declarations.flatMap { it.collectFunctions() }
        else -> emptyList()
    }


private fun <T> List<T>.elementToIdMap(): Map<T, Int> =
    mapIndexed { idx, el -> el to idx }.toMap()

fun IrClass.getSuperClass(builtIns: IrBuiltIns): IrClass? =
    when (this) {
        builtIns.anyClass.owner -> null
        else -> {
            superTypes
                .map { it.classifierOrFail.owner as IrClass }
                .filter { !it.isInterface }
                .singleOrNull() ?: builtIns.anyClass.owner
        }
    }

fun IrClass.allFields(builtIns: IrBuiltIns): List<IrField> =
    getSuperClass(builtIns)?.allFields(builtIns).orEmpty() + declarations.filterIsInstance<IrField>()

class IrModuleToWasm(private val backendContext: WasmBackendContext) {

    private val anyClass = backendContext.irBuiltIns.anyClass.owner

    fun generateModule(module: IrModuleFragment): WasmCompilerResult {
        val fragment = WasmCompiledModuleFragment()
        val generator = WasmCodeGenerator(backendContext, fragment)
        generator.generateModule(module)
        generator.generatePackageFragment(backendContext.internalPackageFragment)

        val compiledModule = fragment.linkWasmCompiledFragments()
        compiledModule.calculateIds()

        val watGenerator = WatGenerator(compiledModule)
        with(watGenerator) {
            compiledModule.wat()
        }

        val wat = watGenerator.builder.toString()

        return WasmCompilerResult(wat, generateStringLiteralsSupport(fragment.stringLiterals))
    }

    private fun generateStringLiteralsSupport(literals: List<String>): String {
        return JsBlock(
            jsAssignment(
                JsNameRef("stringLiterals", "runtime"),
                JsArrayLiteral(literals.map { JsStringLiteral(it) })
            ).makeStmt()
        ).toString()
    }
}

fun IrFunction.isExported(context: WasmCodegenContext): Boolean =
    visibility == Visibilities.PUBLIC && fqNameWhenAvailable in context.backendContext.additionalExportedDeclarations
