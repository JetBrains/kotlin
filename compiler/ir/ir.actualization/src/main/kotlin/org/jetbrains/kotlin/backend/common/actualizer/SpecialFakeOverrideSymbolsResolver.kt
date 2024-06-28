/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrFakeOverrideSymbolBase
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldFakeOverrideSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFunctionFakeOverrideSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertyFakeOverrideSymbol
import org.jetbrains.kotlin.ir.util.SymbolRemapper
import org.jetbrains.kotlin.ir.util.isGetter
import org.jetbrains.kotlin.ir.util.isSetter
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment

/**
 * This class provides utility to resolve [org.jetbrains.kotlin.ir.symbols.impl.IrFunctionFakeOverrideSymbol]
 * and [org.jetbrains.kotlin.ir.symbols.impl.IrPropertyFakeOverrideSymbol] to normal symbols.
 *
 * It can be used after classifiers are actualized and fake overrides are built.
 *
 * Conceptually, a fake override symbol is a pair of real symbol and class in which we need to find this fake override.
 *
 * When the first remapping request comes for a class, all its supertypes are traversed recursively and for all declarations inside,
 * all overrides are cached.
 *
 * This approach is quadratic over the height of class hierarchy. Unfortunately, we need all overrides, not only direct ones
 * (and not only direct-real ones). Because some intermediate overrides can appear in the process of actualization,
 * and we can't guarantee that all real symbols are some specific preferred overrides, as they were right after Fir2Ir.
 *
 */
class SpecialFakeOverrideSymbolsResolver(val expectActualMap: IrExpectActualMap) : SymbolRemapper.Empty() {
    /**
     * Map from (class, declaration) -> declarationInsideClass
     *
     * Means that declarationInsideClass is the one overriding this declaration in this class.
     * [processClass] function add all valid pairs for this class to the map.
     */
    private val cachedFakeOverrides = mutableMapOf<Pair<IrClassSymbol, IrSymbol>, IrSymbol>()
    private val processedClasses = mutableSetOf<IrClass>()

    override fun getReferencedFunction(symbol: IrFunctionSymbol): IrFunctionSymbol {
        return symbol.remap()
    }

    override fun getReferencedSimpleFunction(symbol: IrSimpleFunctionSymbol): IrSimpleFunctionSymbol {
        return symbol.remap()
    }

    override fun getReferencedProperty(symbol: IrPropertySymbol): IrPropertySymbol {
        return symbol.remap()
    }

    override fun getReferencedField(symbol: IrFieldSymbol): IrFieldSymbol {
        if (symbol !is IrFieldFakeOverrideSymbol) return symbol
        val remappedProperty = symbol.correspondingPropertySymbol.remap()
        val remappedBackingField = remappedProperty.owner.backingField
        requireWithAttachment(
            remappedBackingField != null,
            { "Remapped property for f/o field doesn't contain backing field" }
        ) {
            withEntry("originalField", symbol.originalSymbol.owner.render())
            withEntry("containingClass", symbol.containingClassSymbol.owner.render())
            withEntry("remappedProperty", remappedProperty.owner.render())
        }
        return remappedBackingField.symbol
    }

    private inline fun <reified S : IrSymbol> S.remap(): S {
        if (this !is IrFakeOverrideSymbolBase<*, *, *>) {
            return this
        }
        val actualizedClassSymbol = containingClassSymbol.actualize()
        val actualizedOriginalSymbol = originalSymbol.actualize()
        processClass(actualizedClassSymbol.owner)
        when (val result = cachedFakeOverrides[actualizedClassSymbol to actualizedOriginalSymbol]) {
            null -> {
                if (originalSymbol in expectActualMap.propertyAccessorsActualizedByFields) {
                    // This is an accessor of an expect property actualized by a Java field. Skip for now.
                    // It will be handled later in SpecialFakeOverrideSymbolsActualizedByFieldsTransformer.
                    return this
                }
                error("No override for $actualizedOriginalSymbol in $actualizedClassSymbol")
            }
            !is S -> error("Override for $actualizedOriginalSymbol in $actualizedClassSymbol has incompatible type: $result")
            else -> return result
        }
    }

    private fun IrClassSymbol.actualize(): IrClassSymbol {
        return (this as IrSymbol).actualize() as IrClassSymbol
    }

    private fun IrSymbol.actualize(): IrSymbol {
        return expectActualMap.regularSymbols[this] ?: this
    }

    private fun processClass(irClass: IrClass) {
        require(!irClass.isExpect) { "There should be no references to expect classes at this point" }
        if (!processedClasses.add(irClass)) return
        for (declaration in irClass.declarations) {
            if (declaration !is IrOverridableDeclaration<*>) continue
            processDeclaration(irClass.symbol, declaration)
            if (declaration is IrProperty) {
                declaration.getter?.let { processDeclaration(irClass.symbol, it) }
                declaration.setter?.let { processDeclaration(irClass.symbol, it) }
            }
        }
    }

    private fun processDeclaration(classSymbol: IrClassSymbol, declaration: IrOverridableDeclaration<*>) {
        for (overridden in declaration.collectOverrides(mutableSetOf())) {
            cachedFakeOverrides[classSymbol to overridden] = declaration.symbol
        }
    }

    private fun IrOverridableDeclaration<*>.collectOverrides(visited: MutableSet<IrSymbol>): Sequence<IrSymbol> = sequence {
        if (visited.add(symbol)) {
            yield(symbol)
            for (overridden in overriddenSymbols) {
                yieldAll((overridden.remap().owner as IrOverridableDeclaration<*>).collectOverrides(visited))
            }
        }
    }

    fun cacheFakeOverridesOfAllClasses(irModuleFragment: IrModuleFragment) {
        val visitor = object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {}

            override fun visitFile(declaration: IrFile) {
                declaration.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                if (!declaration.isExpect) {
                    processClass(declaration)
                }
                declaration.acceptChildrenVoid(this)
            }
        }
        irModuleFragment.acceptChildrenVoid(visitor)
    }
}


class SpecialFakeOverrideSymbolsResolverVisitor(private val resolver: SpecialFakeOverrideSymbolsResolver) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        // E.g. annotation can contain fake override of constant property
        if (element is IrAnnotationContainer) {
            for (annotation in element.annotations) {
                annotation.acceptVoid(this)
            }
        }
        element.acceptChildrenVoid(this)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        declaration.overriddenSymbols = declaration.overriddenSymbols.map(resolver::getReferencedSimpleFunction)
        visitElement(declaration)
    }

    override fun visitProperty(declaration: IrProperty) {
        declaration.overriddenSymbols = declaration.overriddenSymbols.map(resolver::getReferencedProperty)
        visitElement(declaration)
    }

    override fun visitCall(expression: IrCall) {
        expression.symbol = resolver.getReferencedSimpleFunction(expression.symbol)
        visitElement(expression)
    }

    override fun visitFunctionReference(expression: IrFunctionReference) {
        expression.symbol = resolver.getReferencedFunction(expression.symbol)
        expression.reflectionTarget = expression.reflectionTarget?.let(resolver::getReferencedFunction)
        visitElement(expression)
    }

    override fun visitPropertyReference(expression: IrPropertyReference) {
        val remappedPropertySymbol = expression.symbol.let(resolver::getReferencedProperty)
        expression.symbol = remappedPropertySymbol

        val remappedProperty = remappedPropertySymbol.owner
        if (remappedProperty.isPropertyForJavaField()) {
            val remappedBackingField = remappedProperty.backingField
            requireWithAttachment(
                remappedBackingField != null,
                { "Remapped property for f/o field doesn't contain backing field" }
            ) {
                when (val originalPropertySymbol = expression.symbol) {
                    is IrPropertyFakeOverrideSymbol -> {
                        withEntry("originalProperty", originalPropertySymbol.originalSymbol.owner.render())
                        withEntry("containingClass", originalPropertySymbol.containingClassSymbol.owner.render())
                    }
                    else -> withEntry("originalProperty", originalPropertySymbol.owner.render())
                }
                withEntry("remappedProperty", remappedProperty.render())
            }

            expression.getter = null
            expression.setter = null
            expression.field = remappedBackingField.symbol
        } else {
            expression.getter = expression.getter?.let(resolver::getReferencedSimpleFunction)
            expression.setter = expression.setter?.let(resolver::getReferencedSimpleFunction)
            expression.field = expression.field?.let(resolver::getReferencedField)
        }

        visitElement(expression)
    }

    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference) {
        expression.getter = expression.getter.let(resolver::getReferencedSimpleFunction)
        expression.setter = expression.setter?.let(resolver::getReferencedSimpleFunction)
        visitElement(expression)
    }

    override fun visitFieldAccess(expression: IrFieldAccessExpression) {
        expression.symbol = expression.symbol.let(resolver::getReferencedField)
        visitElement(expression)
    }
}

class SpecialFakeOverrideSymbolsActualizedByFieldsTransformer(
    private val expectActualMap: IrExpectActualMap
) : IrElementTransformerVoid() {
    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid()

        val fakeOverrideAccessorSymbol = expression.symbol as? IrFunctionFakeOverrideSymbol ?: return expression
        val originalAccessorSymbol = fakeOverrideAccessorSymbol.originalSymbol
        val actualizedClassSymbol = fakeOverrideAccessorSymbol.containingClassSymbol

        val actualFieldSymbol = expectActualMap.propertyAccessorsActualizedByFields[originalAccessorSymbol]?.owner?.backingField?.symbol
            ?: error("No override for $originalAccessorSymbol in $actualizedClassSymbol")

        return when {
            originalAccessorSymbol.owner.isGetter -> IrGetFieldImpl(
                expression.startOffset, expression.endOffset,
                symbol = actualFieldSymbol,
                type = expression.type,
                receiver = expression.dispatchReceiver,
                origin = expression.origin,
                superQualifierSymbol = expression.superQualifierSymbol
            )

            originalAccessorSymbol.owner.isSetter -> IrSetFieldImpl(
                expression.startOffset, expression.endOffset,
                symbol = actualFieldSymbol,
                receiver = expression.dispatchReceiver,
                value = expression.getValueArgument(0)!!,
                type = expression.type,
                origin = expression.origin,
                superQualifierSymbol = expression.superQualifierSymbol
            )

            else -> error("Non-property accessor $originalAccessorSymbol actualized by field $actualFieldSymbol in $actualizedClassSymbol")
        }
    }
}
