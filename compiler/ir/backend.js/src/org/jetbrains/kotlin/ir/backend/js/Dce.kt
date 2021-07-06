/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.ir.isMemberOfOpenClass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.js.config.RuntimeDiagnostic
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

fun eliminateDeadDeclarations(
    modules: Iterable<IrModuleFragment>,
    context: JsIrBackendContext
) {

    val allRoots = context.irFactory.stageController.withInitialIr { buildRoots(modules, context) }

    val usefulDeclarations = usefulDeclarations(allRoots, context)

    context.irFactory.stageController.unrestrictDeclarationListsAccess {
        processUselessDeclarations(
            modules,
            usefulDeclarations,
            context
        )
    }
}

private fun IrField.isConstant(): Boolean {
    return correspondingPropertySymbol?.owner?.isConst ?: false
}

private fun IrDeclaration.addRootsTo(to: MutableCollection<IrDeclaration>, context: JsIrBackendContext) {
    when {
        this is IrProperty -> {
            backingField?.addRootsTo(to, context)
            getter?.addRootsTo(to, context)
            setter?.addRootsTo(to, context)
        }
        isEffectivelyExternal() -> {
            to += this
        }
        isExported(context) -> {
            to += this
        }
        this is IrField -> {
            // TODO: simplify
            if ((initializer != null && !isKotlinPackage() || correspondingPropertySymbol?.owner?.isExported(context) == true) && !isConstant()) {
                to += this
            }
        }
        this is IrSimpleFunction -> {
            if (correspondingPropertySymbol?.owner?.isExported(context) == true) {
                to += this
            }
        }
    }
}

private fun buildRoots(modules: Iterable<IrModuleFragment>, context: JsIrBackendContext): Iterable<IrDeclaration> {
    val rootDeclarations = mutableListOf<IrDeclaration>()
    val allFiles = (modules.flatMap { it.files } + context.packageLevelJsModules + context.externalPackageFragment.values)
    allFiles.forEach {
        it.declarations.forEach {
            it.addRootsTo(rootDeclarations, context)
        }
    }

    rootDeclarations += context.testRoots.values

    val dceRuntimeDiagnostic = context.dceRuntimeDiagnostic
    if (dceRuntimeDiagnostic != null) {
        rootDeclarations += dceRuntimeDiagnostic.unreachableDeclarationMethod(context).owner
    }

    if (context.legacyPropertyAccess) {
        rootDeclarations += context.intrinsics.safePropertyGet.owner
        rootDeclarations += context.intrinsics.safePropertySet.owner
    }

    JsMainFunctionDetector.getMainFunctionOrNull(modules.last())?.let { mainFunction ->
        rootDeclarations += mainFunction
        if (mainFunction.isSuspend) {
            rootDeclarations += context.coroutineEmptyContinuation.owner
        }
    }

    return rootDeclarations
}


private fun processUselessDeclarations(
    modules: Iterable<IrModuleFragment>,
    usefulDeclarations: Set<IrDeclaration>,
    context: JsIrBackendContext
) {
    modules.forEach { module ->
        module.files.forEach {
            it.acceptVoid(object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitFile(declaration: IrFile) {
                    process(declaration)
                }

                private fun IrConstructorCall.shouldKeepAnnotation(): Boolean {
                    associatedObject()?.let { obj ->
                        if (obj !in usefulDeclarations) return false
                    }
                    return true
                }

                override fun visitClass(declaration: IrClass) {
                    process(declaration)
                    // Remove annotations for `findAssociatedObject` feature, which reference objects eliminated by the DCE.
                    // Otherwise `JsClassGenerator.generateAssociatedKeyProperties` will try to reference the object factory (which is removed).
                    // That will result in an error from the Namer. It cannot generate a name for an absent declaration.
                    if (declaration.annotations.any { !it.shouldKeepAnnotation() }) {
                        declaration.annotations = declaration.annotations.filter { it.shouldKeepAnnotation() }
                    }
                }

                // TODO bring back the primary constructor fix

                private fun process(container: IrDeclarationContainer) {
                    container.declarations.transformFlat { member ->
                        if (member !in usefulDeclarations) {
                            member.processUselessDeclaration(context)
                        } else {
                            member.acceptVoid(this)
                            null
                        }
                    }
                }
            })
        }
    }
}

private fun IrDeclaration.processUselessDeclaration(context: JsIrBackendContext): List<IrDeclaration>? {
    return when {
        context.dceRuntimeDiagnostic != null -> {
            processWithDiagnostic(context)
            return null
        }
        else -> emptyList()
    }
}

private fun RuntimeDiagnostic.unreachableDeclarationMethod(context: JsIrBackendContext) =
    when (this) {
        RuntimeDiagnostic.LOG -> context.intrinsics.jsUnreachableDeclarationLog
        RuntimeDiagnostic.EXCEPTION -> context.intrinsics.jsUnreachableDeclarationException
    }

private fun RuntimeDiagnostic.removingBody(): Boolean {
    return this != RuntimeDiagnostic.LOG
}

private fun IrDeclaration.processWithDiagnostic(context: JsIrBackendContext) {
    when (this) {
        is IrFunction -> processFunctionWithDiagnostic(context)
        is IrField -> processFieldWithDiagnostic()
        is IrDeclarationContainer -> declarations.forEach { it.processWithDiagnostic(context) }
    }
}

private fun IrFunction.processFunctionWithDiagnostic(context: JsIrBackendContext) {
    val dceRuntimeDiagnostic = context.dceRuntimeDiagnostic!!

    val isRemovingBody = dceRuntimeDiagnostic.removingBody()
    val targetMethod = dceRuntimeDiagnostic.unreachableDeclarationMethod(context)
    val call = JsIrBuilder.buildCall(
        target = targetMethod,
        type = targetMethod.owner.returnType
    )

    if (isRemovingBody) {
        body = context.irFactory.createBlockBody(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET
        )
    }

    body?.prependFunctionCall(call)
}

private fun IrField.processFieldWithDiagnostic() {
    if (initializer != null && isKotlinPackage()) {
        initializer = null
    }
}

private fun IrField.isKotlinPackage() =
    fqNameWhenAvailable?.asString()?.startsWith("kotlin") == true

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

    val classesWithObjectAssociations = hashSetOf<IrClass>()
    val referencedJsClasses = hashSetOf<IrDeclaration>()
    val referencedJsClassesFromExpressions = hashSetOf<IrClass>()

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
    context.irFactory.stageController.withInitialIr {
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

                    override fun visitDeclaration(declaration: IrDeclarationBase) {
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

            fun IrDeclaration.enqueue(description: String, isContagious: Boolean = true) {
                enqueue(declaration, description, isContagious)
            }

            if (declaration is IrClass) {
                declaration.superTypes.forEach {
                    (it.classifierOrNull as? IrClassSymbol)?.owner?.enqueue("superTypes")
                }

                if (declaration.isObject && declaration.isExported(context)) {
                    context.mapping.objectToGetInstanceFunction[declaration]!!
                        .enqueue(declaration, "Exported object getInstance function")
                }

                declaration.annotations.forEach {
                    val annotationClass = it.symbol.owner.constructedClass
                    if (annotationClass.isAssociatedObjectAnnotatedAnnotation) {
                        classesWithObjectAssociations += declaration
                        annotationClass.enqueue("@AssociatedObject annotated annotation class")
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

            body?.acceptVoid(object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitFunctionAccess(expression: IrFunctionAccessExpression) {
                    super.visitFunctionAccess(expression)

                    expression.symbol.owner.enqueue("function access")
                }

                override fun visitRawFunctionReference(expression: IrRawFunctionReference) {
                    super.visitRawFunctionReference(expression)

                    expression.symbol.owner.enqueue("raw function access")
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
                            val inlineClass = context.inlineClassesUtils.getInlinedClass(expression.getTypeArgument(0)!!)!!
                            val constructor = inlineClass.declarations.filterIsInstance<IrConstructor>().single { it.isPrimary }
                            constructor.enqueue("intrinsic: jsBoxIntrinsic")
                        }
                        context.intrinsics.jsClass -> {
                            val ref = expression.getTypeArgument(0)!!.classifierOrFail.owner as IrDeclaration
                            ref.enqueue("intrinsic: jsClass")
                            referencedJsClasses += ref
                            // When class reference provided as parameter to external function
                            // It can be instantiated by external JS script
                            // Need to leave constructor for this
                            // https://youtrack.jetbrains.com/issue/KT-46672
                            // TODO: Possibly solution with origin is not so good
                            //  There is option with applying this hack to jsGetKClass
                            if (expression.origin == JsLoweredDeclarationOrigin.CLASS_REFERENCE) {
                                // Maybe we need to filter primary constructor
                                // Although at this time, we should have only primary constructor
                                (ref as IrClass)
                                    .constructors
                                    .forEach {
                                        it.enqueue("intrinsic: jsClass (constructor)")
                                    }
                            }
                        }
                        context.intrinsics.jsGetKClassFromExpression -> {
                            val ref = expression.getTypeArgument(0)?.classOrNull ?: context.irBuiltIns.anyClass
                            referencedJsClassesFromExpressions += ref.owner
                        }
                        context.intrinsics.jsObjectCreate -> {
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
                        context.intrinsics.jsConstruct -> {
                            val callType = expression.getTypeArgument(0)!!
                            val constructor = callType.getClass()!!.primaryConstructor
                            constructor!!.enqueue("ctor call from jsConstruct-intrinsic")
                        }
                        context.intrinsics.es6DefaultType -> {
                            //same as jsClass
                            val ref = expression.getTypeArgument(0)!!.classifierOrFail.owner as IrDeclaration
                            ref.enqueue("intrinsic: jsClass")
                            referencedJsClasses += ref

                            //Generate klass in `val currResultType = resultType || klass`
                            val arg = expression.getTypeArgument(0)!!
                            val klass = arg.getClass()
                            constructedClasses.addIfNotNull(klass)
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

        // Handle objects, constructed via `findAssociatedObject` annotation
        referencedJsClassesFromExpressions += constructedClasses.filterDescendantsOf(referencedJsClassesFromExpressions) // Grow the set of possible results of instance::class expression
        for (klass in classesWithObjectAssociations) {
            if (klass !in referencedJsClasses && klass !in referencedJsClassesFromExpressions) continue

            for (annotation in klass.annotations) {
                val annotationClass = annotation.symbol.owner.constructedClass
                if (annotationClass !in referencedJsClasses) continue

                annotation.associatedObject()?.let { obj ->
                    context.mapping.objectToGetInstanceFunction[obj]?.enqueue(klass, "associated object factory")
                }
            }
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

                // A hack to enforce property lowering.
                // Until a getter is accessed it doesn't get moved to the declaration list.
                if (declaration is IrProperty) {
                    fun IrSimpleFunction.enqueue(description: String) {
                        findOverriddenContagiousDeclaration()?.let { enqueue(it, description) }
                    }

                    declaration.getter?.enqueue("(getter) overrides useful declaration")
                    declaration.setter?.enqueue("(setter) overrides useful declaration")
                }
            }
        }

        context.additionalExportedDeclarations
            .forEach { it.enqueue(null, "from additionalExportedDeclarations", altFromFqn = "<ROOT>") }
    }

    if (printReachabilityInfo) {
        reachabilityInfo.forEach(::println)
    }

    return result
}

private fun Collection<IrClass>.filterDescendantsOf(bases: Collection<IrClass>): Collection<IrClass> {
    val visited = hashSetOf<IrClass>()
    val baseDescendants = hashSetOf<IrClass>()
    baseDescendants += bases

    fun overridesAnyBase(klass: IrClass): Boolean {
        if (klass in baseDescendants) return true
        if (klass in visited) return false

        visited += klass

        klass.superTypes.forEach {
            (it.classifierOrNull as? IrClassSymbol)?.owner?.let {
                if (overridesAnyBase(it)) {
                    baseDescendants += klass
                    return true
                }
            }
        }

        return false
    }

    return this.filter { overridesAnyBase(it) }
}
