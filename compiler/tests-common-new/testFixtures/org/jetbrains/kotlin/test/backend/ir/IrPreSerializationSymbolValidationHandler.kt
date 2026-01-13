/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.backend.js.ReflectionSymbols
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.isAnnotationWithEqualFqName
import org.jetbrains.kotlin.ir.util.isPublishedApi
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.test.backend.handlers.AbstractIrHandler
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KVisibility

abstract class IrSymbolValidationHandler(testServices: TestServices) : AbstractIrHandler(testServices) {
    protected abstract fun getSymbols(irBuiltIns: IrBuiltIns): List<PreSerializationSymbols>

    override fun processModule(module: TestModule, info: IrBackendInput) {
        for (symbols in getSymbols(info.irBuiltIns)) {
            validateContainer(symbols)
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}

    private fun validateContainer(symbolsContainer: Any) {
        val klass = symbolsContainer::class
        for (member in klass.members) {
            if (member is KProperty<*> && (member.visibility == KVisibility.PUBLIC || member.visibility == KVisibility.INTERNAL)) {
                member.getter.call(symbolsContainer).also { result ->
                    validateRecursive(result, klass)
                }
            }
        }
    }

    private fun validateRecursive(result: Any?, klass: KClass<*>) {
        when (result) {
            is PreSerializationKlibSymbols.SharedVariableBoxClassInfo -> validate(result.klass, klass)
            is Iterable<*> -> result.forEach { validateRecursive(it, klass) }
            is Map<*, *> -> result.entries.forEach { (key, value) ->
                validateRecursive(key, klass)
                validateRecursive(value, klass)
            }
            is IrSymbol -> validate(result, klass)
            is Pair<*, *> -> {
                validateRecursive(result.first, klass)
                validateRecursive(result.second, klass)
            }
            is ReflectionSymbols -> validateContainer(result)
            is IrType -> validateRecursive(result.classifierOrNull, klass)
            null, is FqName, is PrimitiveType, is Name, is String -> Unit // do nothing
            else -> error("Unexpected type: ${result::class.qualifiedName}")
        }
    }

    abstract fun validate(symbol: IrSymbol, symbolsClass: KClass<*>)

    protected fun checkForSpecialAnnotation(declaration: IrDeclaration) {
        val annotations = declaration.annotations +
                (declaration as? IrSimpleFunction)?.correspondingPropertySymbol?.owner?.annotations.orEmpty()
        if (annotations.none { it.symbol.owner.constructedClass.classId == StandardClassIds.Annotations.UsedFromCompilerGeneratedCode }) {
            error("Declaration ${declaration.render()} is not annotated with @${StandardClassIds.Annotations.UsedFromCompilerGeneratedCode}")
        }
    }
}

abstract class IrPreSerializationSymbolValidationHandler(testServices: TestServices) : IrSymbolValidationHandler(testServices) {
    override fun validate(symbol: IrSymbol, symbolsClass: KClass<*>) {
        val owner = symbol.owner as IrDeclarationWithVisibility
        validateVisibility(owner, symbolsClass)
    }

    private fun validateVisibility(declaration: IrDeclarationWithVisibility, symbolsClass: KClass<*>) {
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
}

class IrPreSerializationJsSymbolValidationHandler(testServices: TestServices) : IrPreSerializationSymbolValidationHandler(testServices) {
    override fun getSymbols(irBuiltIns: IrBuiltIns): List<PreSerializationSymbols> {
        return listOf(PreSerializationJsSymbols.Impl(irBuiltIns))
    }
}

class IrPreSerializationWasmSymbolValidationHandler(testServices: TestServices) : IrPreSerializationSymbolValidationHandler(testServices) {
    override fun getSymbols(irBuiltIns: IrBuiltIns): List<PreSerializationSymbols> {
        return listOf(PreSerializationWasmSymbols.Impl(irBuiltIns))
    }
}

class IrPreSerializationNativeSymbolValidationHandler(testServices: TestServices) : IrPreSerializationSymbolValidationHandler(testServices) {
    override fun getSymbols(irBuiltIns: IrBuiltIns): List<PreSerializationSymbols> {
        return listOf(PreSerializationNativeSymbols.Impl(irBuiltIns))
    }
}

abstract class IrSecondPhaseSymbolValidationHandler(testServices: TestServices) : IrSymbolValidationHandler(testServices) {
    override fun validate(symbol: IrSymbol, symbolsClass: KClass<*>) {
        val owner = symbol.owner as IrDeclarationWithVisibility
        validateVisibility(owner, symbolsClass)
    }

    private fun validateVisibility(declaration: IrDeclarationWithVisibility, symbolsClass: KClass<*>) {
        if (declaration.visibility == DescriptorVisibilities.INTERNAL) {
            checkForSpecialAnnotation(declaration)
        }

        (declaration.parentClassOrNull)?.let { validateVisibility(it, symbolsClass) }
    }
}
