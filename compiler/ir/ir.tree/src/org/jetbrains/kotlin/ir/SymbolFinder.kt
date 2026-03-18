/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(InternalSymbolFinderAPI::class)

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId

@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Target(AnnotationTarget.CLASS)
annotation class InternalSymbolFinderAPI

@InternalSymbolFinderAPI
abstract class SymbolFinder {
    abstract fun findFunctions(callableId: CallableId): Iterable<IrSimpleFunctionSymbol>
    abstract fun findProperties(callableId: CallableId): Iterable<IrPropertySymbol>
    abstract fun findClass(classId: ClassId): IrClassSymbol?
}

@InternalSymbolFinderAPI
interface SymbolFinderHolder {
    val symbolFinder: SymbolFinder
    val languageVersionSettings: LanguageVersionSettings
}

context(holder: SymbolFinderHolder)
fun ClassId.classSymbolOrNull(): IrClassSymbol? = holder.symbolFinder.findClass(this)

context(holder: SymbolFinderHolder)
fun ClassId.classSymbol(): IrClassSymbol = classSymbolOrNull() ?: error("Class $this is not found")

context(holder: SymbolFinderHolder)
fun ClassId.lazyClassSymbol(languageFeature: LanguageFeature): Lazy<IrClassSymbol> =
    classSymbolOrNull().let { classSymbol ->
        lazy {
            if (!holder.languageVersionSettings.supportsFeature(languageFeature))
                error("Class $this cannot be loaded when language feature '$languageFeature' is not enabled")
            classSymbol ?: error("Class $this is not found, but language feature '$languageFeature' that requires this symbol is enabled")
        }
    }

context(holder: SymbolFinderHolder)
fun CallableId.propertySymbols(): List<IrPropertySymbol> = holder.symbolFinder.findProperties(this).toList()

context(holder: SymbolFinderHolder)
fun CallableId.functionSymbols(): List<IrSimpleFunctionSymbol> = holder.symbolFinder.findFunctions(this).toList()

context(holder: SymbolFinderHolder)
fun ClassId.primaryConstructorSymbol(): Lazy<IrConstructorSymbol> {
    val clazz = classSymbol()
    return lazy { (clazz.owner.primaryConstructor ?: error("Class ${this} has no primary constructor")).symbol }
}

context(holder: SymbolFinderHolder)
fun ClassId.noParametersConstructorSymbol(): Lazy<IrConstructorSymbol> {
    val clazz = classSymbol()
    return lazy { (clazz.owner.constructors.singleOrNull { it.parameters.isEmpty() } ?: error("Class ${this} has no constructor without parameters")).symbol }
}

context(holder: SymbolFinderHolder)
fun ClassId.defaultType(): Lazy<IrType> {
    val clazz = classSymbol()
    return lazy { clazz.defaultType }
}

context(holder: SymbolFinderHolder)
fun CallableId.propertySymbol(): IrPropertySymbol {
    val elements = propertySymbols()
    require(elements.isNotEmpty()) { "No property $this found" }
    require(elements.size == 1) {
        "Several properties $this found:\n${elements.joinToString("\n")}"
    }
    return elements.single()
}

context(holder: SymbolFinderHolder)
fun CallableId.functionSymbolOrNull(): IrSimpleFunctionSymbol? {
    val elements = functionSymbols()
    require(elements.size <= 1) {
        "Several functions $this found:\n${elements.joinToString("\n")}\nTry using functionSymbol(condition) instead to filter" }
    return elements.singleOrNull()
}

context(holder: SymbolFinderHolder)
fun CallableId.functionSymbol(): IrSimpleFunctionSymbol {
    val elements = functionSymbols()
    require(elements.isNotEmpty()) { "No function $this found" }
    require(elements.size == 1) {
        "Several functions $this found:\n${elements.joinToString("\n")}\nTry using functionSymbol(condition) instead to filter" }
    return elements.single()
}

context(holder: SymbolFinderHolder)
inline fun CallableId.functionSymbol(crossinline condition: (IrSimpleFunction) -> Boolean): Lazy<IrSimpleFunctionSymbol> {
    val unfilteredElements = functionSymbols()
    return lazy {
        val elements = unfilteredElements.filter { condition(it.owner) }
        require(elements.isNotEmpty()) { "No function $this found corresponding given condition" }
        require(elements.size == 1) { "Several functions $this found corresponding given condition:\n${elements.joinToString("\n")}" }
        elements.single()
    }
}

context(holder: SymbolFinderHolder)
inline fun <K> CallableId.functionSymbolAssociatedBy(
    crossinline condition: (IrSimpleFunction) -> Boolean = { true },
    crossinline getKey: (IrSimpleFunction) -> K
): Lazy<Map<K, IrSimpleFunctionSymbol>> {
    val unfilteredElements = functionSymbols()
    return lazy {
        val elements = unfilteredElements.filter { condition(it.owner) }
        elements.associateBy { getKey(it.owner) }
    }
}

context(holder: SymbolFinderHolder)
fun CallableId.getterSymbol(): Lazy<IrSimpleFunctionSymbol> {
    val elements = propertySymbols()
    require(elements.isNotEmpty()) { "No properties $this found" }
    require(elements.size == 1) { "Several properties $this found:\n${elements.joinToString("\n")}" }
    return lazy {
        elements.single().owner.getter!!.symbol
    }
}

context(holder: SymbolFinderHolder)
fun CallableId.setterSymbol(): Lazy<IrSimpleFunctionSymbol> {
    val elements = propertySymbols()
    require(elements.isNotEmpty()) { "No properties $this found" }
    require(elements.size == 1) { "Several properties $this found:\n${elements.joinToString("\n")}" }
    return lazy {
        elements.single().owner.setter!!.symbol
    }
}

context(holder: SymbolFinderHolder)
fun CallableId.getterSymbol(extensionReceiverClass: IrClassSymbol?): Lazy<IrSimpleFunctionSymbol> {
    val unfilteredElements = propertySymbols()
    require(unfilteredElements.isNotEmpty()) { "No properties $this found" }
    return lazy {
        val elements = unfilteredElements.filter { it.owner.getter?.extensionReceiverClass == extensionReceiverClass }
        require(elements.isNotEmpty()) { "No properties $this found with ${extensionReceiverClass} receiver" }
        require(elements.size == 1) { "Several properties $this found with ${extensionReceiverClass} receiver:\n${elements.joinToString("\n")}" }
        elements.single().owner.getter!!.symbol
    }
}

context(holder: SymbolFinderHolder) val IrFunction.extensionReceiverType: IrType? get() = parameters.singleOrNull { it.kind == IrParameterKind.ExtensionReceiver }?.type
context(holder: SymbolFinderHolder) val IrFunction.extensionReceiverClass: IrClassSymbol? get() = extensionReceiverType?.classOrNull
