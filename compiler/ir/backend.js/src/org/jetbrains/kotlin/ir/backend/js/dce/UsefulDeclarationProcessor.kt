/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.dce

import org.jetbrains.kotlin.backend.common.ir.inlineFunction
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.utils.hasJsPolyfill
import org.jetbrains.kotlin.ir.backend.js.utils.isAssociatedObjectAnnotatedAnnotation
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import java.util.*

abstract class UsefulDeclarationProcessor(
    private val printReachabilityInfo: Boolean,
    protected val removeUnusedAssociatedObjects: Boolean
) {
    abstract val context: JsCommonBackendContext

    protected fun getMethodOfAny(name: String): IrDeclaration =
        context.irBuiltIns.anyClass.owner.declarations.filterIsInstance<IrFunction>().single { it.name.asString() == name }

    protected val toStringMethod: IrDeclaration by lazy { getMethodOfAny("toString") }
    protected abstract fun isExported(declaration: IrDeclaration): Boolean
    protected abstract val bodyVisitor: BodyVisitorBase

    protected abstract inner class BodyVisitorBase : IrElementVisitor<Unit, IrDeclaration> {
        override fun visitValueAccess(expression: IrValueAccessExpression, data: IrDeclaration) {
            visitDeclarationReference(expression, data)
            expression.symbol.owner.enqueue(data, "variable access")
        }

        override fun visitElement(element: IrElement, data: IrDeclaration) {
            element.acceptChildren(this, data)
        }

        override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: IrDeclaration) {
            super.visitFunctionAccess(expression, data)
            expression.symbol.owner.enqueue(data, "function access")
        }

        override fun visitRawFunctionReference(expression: IrRawFunctionReference, data: IrDeclaration) {
            super.visitRawFunctionReference(expression, data)
            expression.symbol.owner.enqueue(data, "raw function access")
        }

        override fun visitBlock(expression: IrBlock, data: IrDeclaration) {
            super.visitBlock(expression, data)

            if (expression is IrReturnableBlock) {
                expression.inlineFunction?.addToUsefulPolyfilledDeclarations()
            }
        }

        override fun visitFieldAccess(expression: IrFieldAccessExpression, data: IrDeclaration) {
            super.visitFieldAccess(expression, data)

            val field = expression.symbol.owner.apply { enqueue(data, "field access") }
            val correspondingProperty = field.correspondingPropertySymbol?.owner ?: return

            if (
                field.origin == IrDeclarationOrigin.PROPERTY_BACKING_FIELD &&
                correspondingProperty.hasJsPolyfill()
            ) {
                correspondingProperty.enqueue(field, "property backing field")
            }
        }

        override fun visitStringConcatenation(expression: IrStringConcatenation, data: IrDeclaration) {
            super.visitStringConcatenation(expression, data)
            toStringMethod.enqueue(data, "string concatenation")
        }
    }

    private fun addReachabilityInfoIfNeeded(
        from: IrDeclaration,
        to: IrDeclaration,
        description: String?,
        isContagiousOverridableDeclaration: Boolean,
    ) {
        if (!printReachabilityInfo) return
        val fromFqn = (from as? IrDeclarationWithName)?.fqNameWhenAvailable?.asString() ?: "<unknown>"
        val toFqn = (to as? IrDeclarationWithName)?.fqNameWhenAvailable?.asString() ?: "<unknown>"
        val comment = (description ?: "") + (if (isContagiousOverridableDeclaration) "[CONTAGIOUS!]" else "")
        val info = "\"$fromFqn\" -> \"$toFqn\"" + (if (comment.isBlank()) "" else " // $comment")
        reachabilityInfo.add(info)
    }

    protected fun IrDeclaration.enqueue(
        from: IrDeclaration,
        description: String?,
        isContagious: Boolean = true,
    ) {
        // Ignore non-external IrProperty because we don't want to generate code for them and codegen doesn't support it.
        if (this is IrProperty && !this.isExternal) return

        // TODO check that this is overridable
        // it requires fixing how functions with default arguments is handled
        val isContagiousOverridableDeclaration = isContagious && this is IrOverridableDeclaration<*> && this.isMemberOfOpenClass

        addReachabilityInfoIfNeeded(from, this, description, isContagiousOverridableDeclaration)

        if (isContagiousOverridableDeclaration) {
            contagiousReachableDeclarations.add(this as IrOverridableDeclaration<*>)
        }

        if (this !in result) {
            result.add(this)
            queue.addLast(this)

            addToUsefulPolyfilledDeclarations()
        }
    }

    private fun IrDeclaration.addToUsefulPolyfilledDeclarations() {
        if (hasJsPolyfill() && this !in usefulPolyfilledDeclarations) {
            usefulPolyfilledDeclarations.add(this)
        }
    }

    // This collection contains declarations whose reachability should be propagated to overrides.
    // Overriding uncontagious declaration will not lead to becoming a declaration reachable.
    // By default, all declarations treated as contagious, it's not the most efficient, but it's safest.
    // In case when we access a declaration through a fake-override declaration, the original (real) one will not be marked as contagious,
    // so, later, other overrides will not be processed unconditionally only because it overrides a reachable declaration.
    //
    // The collection must be a subset of [result] set.
    private val contagiousReachableDeclarations = hashSetOf<IrOverridableDeclaration<*>>()
    protected val constructedClasses = hashSetOf<IrClass>()
    private val reachabilityInfo: MutableSet<String> = if (printReachabilityInfo) linkedSetOf() else Collections.emptySet()
    private val queue = ArrayDeque<IrDeclaration>()
    protected val result = hashSetOf<IrDeclaration>()
    protected val classesWithObjectAssociations = hashSetOf<IrClass>()

    val usefulPolyfilledDeclarations = hashSetOf<IrDeclaration>()

    protected open fun processField(irField: IrField): Unit = Unit

    protected open fun processClass(irClass: IrClass) {
        processSuperTypes(irClass)

        if (irClass.isObject && isExported(irClass)) {
            context.mapping.objectToGetInstanceFunction[irClass]
                ?.enqueue(irClass, "Exported object getInstance function")
        }

        irClass.annotations.forEach {
            val annotationClass = it.symbol.owner.constructedClass
            if (annotationClass.isAssociatedObjectAnnotatedAnnotation) {
                classesWithObjectAssociations += irClass
                annotationClass.enqueue(irClass, "@AssociatedObject annotated annotation class")
            }
        }
    }

    protected open fun processSuperTypes(irClass: IrClass) {
        irClass.superTypes.forEach {
            (it.classifierOrNull as? IrClassSymbol)?.owner?.enqueue(irClass, "superTypes")
        }
    }

    protected open fun processSimpleFunction(irFunction: IrSimpleFunction) {
        if (irFunction.isFakeOverride) {
            irFunction.resolveFakeOverride()?.enqueue(irFunction, "real overridden fun", isContagious = false)
        }
    }

    protected open fun processConstructor(irConstructor: IrConstructor) {
        // Collect instantiated classes.
        irConstructor.constructedClass.let {
            it.enqueue(irConstructor, "constructed class")
            constructedClasses += it
        }
    }

    protected open fun processConstructedClassDeclaration(declaration: IrDeclaration) {
        if (declaration in result) return

        fun IrOverridableDeclaration<*>.findOverriddenContagiousDeclaration(): IrOverridableDeclaration<*>? {
            for (overriddenSymbol in this.overriddenSymbols) {
                val overriddenDeclaration = overriddenSymbol.owner as? IrOverridableDeclaration<*> ?: continue

                if (overriddenDeclaration in contagiousReachableDeclarations) return overriddenDeclaration

                overriddenDeclaration.findOverriddenContagiousDeclaration()?.let {
                    return it
                }
            }
            return null
        }

        if (declaration is IrOverridableDeclaration<*>) {
            declaration.findOverriddenContagiousDeclaration()?.let {
                declaration.enqueue(it, "overrides useful declaration")
            }
        }

        if (declaration is IrSimpleFunction && declaration.isAccessorForOverriddenExternalField()) {
            declaration.enqueue(declaration.correspondingPropertySymbol!!.owner, "overrides external property")
        }

        // A hack to enforce property lowering.
        // Until a getter is accessed it doesn't get moved to the declaration list.
        if (declaration is IrProperty) {
            declaration.getter?.run {
                findOverriddenContagiousDeclaration()?.let { enqueue(declaration, "(getter) overrides useful declaration") }
            }
            declaration.setter?.run {
                findOverriddenContagiousDeclaration()?.let { enqueue(declaration, "(setter) overrides useful declaration") }
            }
        }
    }

    protected fun IrSimpleFunction.isAccessorForOverriddenExternalField(): Boolean {
        return correspondingPropertySymbol?.owner?.isExternalOrOverriddenExternal() ?: false
    }

    protected fun IrProperty.isExternalOrOverriddenExternal(): Boolean {
        return isEffectivelyExternal() || isOverriddenExternal()
    }

    protected fun IrProperty.isOverriddenExternal(): Boolean =
        overriddenSymbols.any { it.owner.isExternalOrOverriddenExternal() }

    protected open fun handleAssociatedObjects(): Unit = Unit

    fun collectDeclarations(rootDeclarations: Iterable<IrDeclaration>): Set<IrDeclaration> {

        rootDeclarations.forEach {
            it.enqueue(it, "<ROOT>")
        }

        while (queue.isNotEmpty()) {
            while (queue.isNotEmpty()) {
                val declaration = queue.pollFirst()

                when (declaration) {
                    is IrClass -> processClass(declaration)
                    is IrSimpleFunction -> processSimpleFunction(declaration)
                    is IrConstructor -> processConstructor(declaration)
                    is IrField -> processField(declaration)
                }

                val body = when (declaration) {
                    is IrFunction -> declaration.body
                    is IrField -> declaration.initializer
                    is IrVariable -> declaration.initializer
                    else -> null
                }

                body?.accept(bodyVisitor, declaration)
            }

            handleAssociatedObjects()

            for (klass in constructedClasses) {
                // TODO a better way to support inverse overrides.
                for (declaration in klass.declarations) {
                    processConstructedClassDeclaration(declaration)
                }
            }
        }

        if (printReachabilityInfo) {
            reachabilityInfo.forEach(::println)
        }

        return result
    }
}