/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.ir.isMemberOfOpenClass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.backend.js.utils.getJsName
import org.jetbrains.kotlin.ir.backend.js.utils.getJsNameOrKotlinName
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import java.util.*

fun eliminateDeadDeclarations(
    module: IrModuleFragment,
    context: JsIrBackendContext,
    mainFunction: IrSimpleFunction?
) {

    val allRoots = stageController.withInitialIr { buildRoots(module, context, mainFunction) }

    val usefulDeclarations = usefulDeclarations(allRoots, context)

    stageController.unrestrictDeclarationListsAccess {
        removeUselessDeclarations(module, usefulDeclarations)
    }
}

private fun IrField.isConstant(): Boolean {
    return correspondingPropertySymbol?.owner?.isConst ?: false
}

private fun buildRoots(module: IrModuleFragment, context: JsIrBackendContext, mainFunction: IrSimpleFunction?): Iterable<IrDeclaration> {
    val rootDeclarations =
        (module.files + context.packageLevelJsModules + context.externalPackageFragment.values).flatMapTo(mutableListOf()) { file ->
            file.declarations.flatMap { if (it is IrProperty) listOfNotNull(it.backingField, it.getter, it.setter) else listOf(it) }
                .filter {
                    it is IrField && it.initializer != null && it.fqNameWhenAvailable?.asString()?.startsWith("kotlin") != true
                            || it.isExported(context)
                            || it.isEffectivelyExternal()
                            || it is IrField && it.correspondingPropertySymbol?.owner?.isExported(context) == true
                            || it is IrSimpleFunction && it.correspondingPropertySymbol?.owner?.isExported(context) == true
                }.filter { !(it is IrField && it.isConstant() && !it.isExported(context)) }
        }

    if (context.hasTests) rootDeclarations += context.testContainer

    if (mainFunction != null) {
        rootDeclarations += mainFunction
        if (mainFunction.isSuspend) {
            rootDeclarations += context.coroutineEmptyContinuation.owner
        }
    }

    return rootDeclarations
}

private fun removeUselessDeclarations(module: IrModuleFragment, usefulDeclarations: Set<IrDeclaration>) {
    module.files.forEach {
        it.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitFile(declaration: IrFile) {
                process(declaration)
            }

            override fun visitClass(declaration: IrClass) {
                process(declaration)
            }

            // TODO bring back the primary constructor fix

            private fun process(container: IrDeclarationContainer) {
                container.declarations.transformFlat { member ->
                    if (member !in usefulDeclarations) {
                        emptyList()
                    } else {
                        member.acceptVoid(this)
                        null
                    }
                }
            }
        })
    }
}

// TODO refactor it, the function became too big. Please contact me (Zalim) before doing it.
fun usefulDeclarations(roots: Iterable<IrDeclaration>, context: JsIrBackendContext): Set<IrDeclaration> {
    val printReachabilityInfo =
        context.configuration.getBoolean(JSConfigurationKeys.PRINT_REACHABILITY_INFO) ||
                java.lang.Boolean.getBoolean("kotlin.js.ir.dce.print.reachability.info")
    val reachabilityInfo: MutableSet<String> = if (printReachabilityInfo) linkedSetOf() else Collections.emptySet()

    val queue = ArrayDeque<IrDeclaration>()
    val result = hashSetOf<IrDeclaration>()

    // This collection contains declarations whose reachability should be propagated to overrides.
    // Overriding uncontagious declaration will not lead to becoming a declaration reachable.
    // By default, all declarations treated as contagious, it's not the most efficient, but it's safest.
    // In case when we access a declaration through a fake-override declaration, the original (real) one will not be marked as contagious,
    // so, later, other overrides will not be processed unconditionally only because it overrides a reachable declaration.
    //
    // The collection must be a subset of [result] set.
    val contagiousReachableDeclarations = hashSetOf<IrOverridableDeclaration<*>>()
    val constructedClasses = hashSetOf<IrClass>()

    fun IrDeclaration.enqueue(
        from: IrDeclaration?,
        description: String?,
        isContagious: Boolean = true,
        altFromFqn: String? = null
    ) {
        // Ignore non-external IrProperty because we don't want to generate code for them and codegen doesn't support it.
        if (this is IrProperty && !this.isExternal) return

        // TODO check that this is overridable
        // it requires fixing how functions with default arguments is handled
        val isContagiousOverridableDeclaration = isContagious && this is IrOverridableDeclaration<*> && this.isMemberOfOpenClass

        if (printReachabilityInfo) {
            val fromFqn = (from as? IrDeclarationWithName)?.fqNameWhenAvailable?.asString() ?: altFromFqn ?: "<unknown>"
            val toFqn = (this as? IrDeclarationWithName)?.fqNameWhenAvailable?.asString() ?: "<unknown>"

            val comment = (description ?: "") + (if (isContagiousOverridableDeclaration) "[CONTAGIOUS!]" else "")
            val v = "\"$fromFqn\" -> \"$toFqn\"" + (if (comment.isBlank()) "" else " // $comment")

            reachabilityInfo.add(v)
        }

        if (isContagiousOverridableDeclaration) {
            contagiousReachableDeclarations.add(this as IrOverridableDeclaration<*>)
        }

        if (this !in result) {
            result.add(this)
            queue.addLast(this)
        }
    }

    // use withInitialIr to avoid ConcurrentModificationException in dce-driven lowering when adding roots' nested declarations (members)
    stageController.withInitialIr {
        // Add roots
        roots.forEach {
            it.enqueue(null, null, altFromFqn = "<ROOT>")
        }

        // Add roots' nested declarations
        roots.forEach {
            it.acceptVoid(
                object : IrElementVisitorVoid {
                    override fun visitElement(element: IrElement) {
                        element.acceptChildrenVoid(this)
                    }

                    override fun visitBody(body: IrBody) {
                        // Skip
                    }

                    override fun visitDeclaration(declaration: IrDeclaration) {
                        if (declaration !== it) declaration.enqueue(it, "roots' nested declaration")

                        super.visitDeclaration(declaration)
                    }
                }
            )
        }
    }

    val toStringMethod =
        context.irBuiltIns.anyClass.owner.declarations.filterIsInstance<IrFunction>().single { it.name.asString() == "toString" }
    val equalsMethod =
        context.irBuiltIns.anyClass.owner.declarations.filterIsInstance<IrFunction>().single { it.name.asString() == "equals" }
    val hashCodeMethod =
        context.irBuiltIns.anyClass.owner.declarations.filterIsInstance<IrFunction>().single { it.name.asString() == "hashCode" }

    while (queue.isNotEmpty()) {
        while (queue.isNotEmpty()) {
            val declaration = queue.pollFirst()

            // TODO remove?
            stageController.lazyLower(declaration)

            fun IrDeclaration.enqueue(description: String, isContagious: Boolean = true) {
                enqueue(declaration, description, isContagious)
            }

            if (declaration is IrClass) {
                declaration.superTypes.forEach {
                    (it.classifierOrNull as? IrClassSymbol)?.owner?.enqueue("superTypes")
                }

                // TODO find out how `doResume` gets removed
                if (declaration.symbol == context.ir.symbols.coroutineImpl) {
                    declaration.declarations.toList().forEach {
                        if (it is IrSimpleFunction && it.name.asString() == "doResume") {
                            it.enqueue("hack for CoroutineImpl::doResume")
                        }
                    }
                }
            }

            if (declaration is IrSimpleFunction && declaration.isFakeOverride) {
                declaration.resolveFakeOverride()?.enqueue("real overridden fun", isContagious = false)
            }

            // Collect instantiated classes.
            if (declaration is IrConstructor) {
                declaration.constructedClass.let {
                    it.enqueue("constructed class")
                    constructedClasses += it
                }
            }

            val body = when (declaration) {
                is IrFunction -> declaration.body
                is IrField -> declaration.initializer
                is IrVariable -> declaration.initializer
                else -> null
            }

            // TODO remove?
            (body as? IrBody)?.let { stageController.lazyLower(it) }

            body?.acceptVoid(object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitFunctionAccess(expression: IrFunctionAccessExpression) {
                    super.visitFunctionAccess(expression)

                    expression.symbol.owner.enqueue("function access")
                }

                override fun visitVariableAccess(expression: IrValueAccessExpression) {
                    super.visitVariableAccess(expression)

                    expression.symbol.owner.enqueue("variable access")
                }

                override fun visitFieldAccess(expression: IrFieldAccessExpression) {
                    super.visitFieldAccess(expression)

                    expression.symbol.owner.enqueue("field access")
                }

                override fun visitCall(expression: IrCall) {
                    super.visitCall(expression)

                    when (expression.symbol) {
                        context.intrinsics.jsBoxIntrinsic -> {
                            val inlineClass = expression.getTypeArgument(0)!!.getInlinedClass()!!
                            val constructor = inlineClass.declarations.filterIsInstance<IrConstructor>().single { it.isPrimary }
                            constructor.enqueue("intrinsic: jsBoxIntrinsic")
                        }
                        context.intrinsics.jsClass -> {
                            (expression.getTypeArgument(0)!!.classifierOrFail.owner as IrDeclaration).enqueue("intrinsic: jsClass")
                        }
                        context.intrinsics.jsObjectCreate.symbol -> {
                            val classToCreate = expression.getTypeArgument(0)!!.classifierOrFail.owner as IrClass
                            classToCreate.enqueue("intrinsic: jsObjectCreate")
                            constructedClasses += classToCreate
                        }
                        context.intrinsics.jsEquals -> {
                            equalsMethod.enqueue("intrinsic: jsEquals")
                        }
                        context.intrinsics.jsToString -> {
                            toStringMethod.enqueue("intrinsic: jsToString")
                        }
                        context.intrinsics.jsHashCode -> {
                            hashCodeMethod.enqueue("intrinsic: jsHashCode")
                        }
                        context.intrinsics.jsPlus -> {
                            if (expression.getValueArgument(0)?.type?.classOrNull == context.irBuiltIns.stringClass) {
                                toStringMethod.enqueue("intrinsic: jsPlus")
                            }
                        }
                    }
                }

                override fun visitStringConcatenation(expression: IrStringConcatenation) {
                    super.visitStringConcatenation(expression)

                    toStringMethod.enqueue("string concatenation")
                }
            })
        }

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

        for (klass in constructedClasses) {
            // TODO a better way to support inverse overrides.
            for (declaration in ArrayList(klass.declarations)) {
                if (declaration in result) continue

                if (declaration is IrOverridableDeclaration<*>) {
                    declaration.findOverriddenContagiousDeclaration()?.let {
                        declaration.enqueue(it, "overrides useful declaration")
                    }
                }

                if (declaration is IrSimpleFunction && declaration.getJsNameOrKotlinName().asString() == "valueOf") {
                    declaration.enqueue(klass, "valueOf")
                }

                // A hack to support `toJson` and other js-specific members
                if (declaration.getJsName() != null ||
                    declaration is IrField && declaration.correspondingPropertySymbol?.owner?.getJsName() != null ||
                    declaration is IrSimpleFunction && declaration.correspondingPropertySymbol?.owner?.getJsName() != null
                ) {
                    declaration.enqueue(klass, "annotated by @JsName")
                }
            }

            // TODO is this needed?
            for (declaration in ArrayList(klass.declarations)) {
                // TODO this is a hack.
                if (declaration is IrProperty) {
                    fun IrSimpleFunction.enqueue(description: String) {
                        findOverriddenContagiousDeclaration()?.let { enqueue(it, description) }
                    }

                    declaration.getter?.enqueue("(getter) overrides useful declaration")
                    declaration.setter?.enqueue("(setter) overrides useful declaration")
                }
            }

            // TODO find out how `doResume` gets removed
            if (klass.symbol == context.ir.symbols.coroutineImpl) {
                ArrayList(klass.declarations).forEach {
                    // TODO: fix the heck
//                    if (it is IrSimpleFunction && it.name.asString() == "doResume") {
                        it.enqueue(klass, "hack for CoroutineImpl::doResume")
//                    }
                }
            }
        }
    }

    if (printReachabilityInfo) {
        reachabilityInfo.forEach(::println)
    }

    return result
}
