/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrDynamicType
import org.jetbrains.kotlin.ir.util.isAnnotationWithEqualFqName
import org.jetbrains.kotlin.ir.util.isPublishedApi
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.backend.handlers.AbstractIrHandler
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KVisibility

abstract class IrPreSerializationSymbolValidationHandler(testServices: TestServices) : AbstractIrHandler(testServices) {
    companion object {
        private val preSerializationAnnotation = FqName.fromSegments(listOf("kotlin", "internal", "UsedFromCompilerGeneratedCode"))
    }

    abstract fun getSymbols(irBuiltIns: IrBuiltIns): PreSerializationSymbols

    override fun processModule(module: TestModule, info: IrBackendInput) {
        validate(getSymbols(info.irBuiltIns))
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}

    private fun validate(symbols: PreSerializationSymbols) {
        val klass = symbols::class
        klass.members.forEach {
            if (it !is KProperty<*> || it.visibility != KVisibility.PUBLIC) return@forEach
            it.getter.call(symbols).also { result ->
                validateRecursive(result, klass)
            }
        }
    }

    private fun validateRecursive(result: Any?, klass: KClass<out PreSerializationSymbols>) {
        when (result) {
            is PreSerializationKlibSymbols.SharedVariableBoxClassInfo -> validate(result.klass, klass)
            is Iterable<*> -> result.forEach { validateRecursive(it, klass) }
            is Map<*, *> -> result.entries.forEach { (key, value) ->
                validateRecursive(key, klass)
                validateRecursive(value, klass)
            }
            is IrSymbol -> validate(result, klass)
            null, is FqName, is IrDynamicType -> Unit // do nothing
            else -> error("Unexpected type: ${result::class.qualifiedName}")
        }
    }

    private fun validate(symbol: IrSymbol, symbolsClass: KClass<out PreSerializationSymbols>) {
        val owner = symbol.owner as IrDeclarationWithVisibility
        validateVisibility(owner, symbolsClass)
    }

    private fun validateVisibility(declaration: IrDeclarationWithVisibility, symbolsClass: KClass<out PreSerializationSymbols>) {
        if (declaration.visibility == DescriptorVisibilities.INTERNAL) {
            require(declaration.isPublishedApi()) {
                "Internal API loaded from ${symbolsClass.qualifiedName} must have '@PublishedApi' annotation: ${declaration.render()}"
            }
            checkForSpecialAnnotation(declaration)
        } else {
            require(declaration.visibility.isPublicAPI) {
                "Symbol loaded from ${symbolsClass.qualifiedName} must be public or internal with '@PublishedApi' annotation: ${declaration.render()}"
            }
        }

        (declaration.parentClassOrNull)?.let { validateVisibility(it, symbolsClass) }
    }

    private fun checkForSpecialAnnotation(declaration: IrDeclaration) {
        if (declaration.annotations.none { it.isAnnotationWithEqualFqName(preSerializationAnnotation) }) {
            error("Declaration ${declaration.render()} is not annotated with @${preSerializationAnnotation.shortName()}")
        }
    }
}

class IrPreSerializationJsSymbolValidationHandler(testServices: TestServices) : IrPreSerializationSymbolValidationHandler(testServices) {
    override fun getSymbols(irBuiltIns: IrBuiltIns): PreSerializationSymbols {
        return PreSerializationJsSymbols.Impl(irBuiltIns)
    }
}

class IrPreSerializationWasmSymbolValidationHandler(testServices: TestServices) : IrPreSerializationSymbolValidationHandler(testServices) {
    override fun getSymbols(irBuiltIns: IrBuiltIns): PreSerializationSymbols {
        return PreSerializationWasmSymbols.Impl(irBuiltIns)
    }
}

class IrPreSerializationNativeSymbolValidationHandler(testServices: TestServices) : IrPreSerializationSymbolValidationHandler(testServices) {
    override fun getSymbols(irBuiltIns: IrBuiltIns): PreSerializationSymbols {
        return PreSerializationNativeSymbols.Impl(irBuiltIns)
    }
}
