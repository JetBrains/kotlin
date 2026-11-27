/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator

import org.jetbrains.kotlin.generators.tree.TypeKind
import org.jetbrains.kotlin.generators.tree.imports.ArbitraryImportable
import org.jetbrains.kotlin.ir.generator.model.*
import org.jetbrains.kotlin.ir.generator.model.ExpressionEntryPoint.*

/**
 * Configures the v2 expression builders (`org.jetbrains.kotlin.ir.builders.expressions.v2`).
 *
 * There is a builder for each class of the IR tree among the expressions (and bodies, branches, catches), and the builders follow
 * the hierarchy of the classes. The properties of a builder are derived from the fields of its element (see [properties]):
 * - [alwaysExcludedFields] and [ExpressionBuilder.excludedFields] are skipped;
 * - identity fields (immutable fields and symbols named `symbol`, plus [ExpressionBuilder.identityFields]) are read-only
 *   properties, parameters of the builder constructor and of the entry points;
 * - other fields are `var`s, or `val`s for mutable lists, with defaults derived from the field (see
 *   [DeclarationBuilderConfigurator.defaultValue]) or [ExpressionBuilder.defaults]; without a default, a property is `lateinit`.
 *   Child expressions are plain properties: they are built before their parent, e.g. `branches += irBranch {}`.
 *
 * The defaults that depend on identity fields (e.g. the type of a call) and on the context (built-ins, the scope) are configured
 * here, the hooks that don't fit an expression (e.g. the shape of the arguments of a call) are hand-written next to the entry points.
 */
object ExpressionBuilderConfigurator {
    private val alwaysExcludedFields = setOf("attributeOwnerId", "descriptor")

    private fun importable(packageName: String, name: String) = ArbitraryImportable(packageName, name)

    private val defaultType = importable(Packages.util, "defaultType")
    private val parentAsClass = importable(Packages.util, "parentAsClass")
    private val undefinedOffset = importable(Packages.tree, "UNDEFINED_OFFSET")

    private fun unit() = Expr("context.irBuiltIns.unitType")
    private fun nothing() = Expr("context.irBuiltIns.nothingType")

    fun configure(model: org.jetbrains.kotlin.generators.tree.Model<Element>): List<ExpressionBuilder> = with(IrTree) {
        val elements = model.elements.filterTo(mutableSetOf()) {
            it.category == Element.Category.Expression && it.kind?.typeKind == TypeKind.Class
        }
        val builders = linkedMapOf<Element, ExpressionBuilder>()
        val root = ExpressionBuilder(rootElement, parent = null).also { builders[rootElement] = it }
        fun builderOf(element: Element): ExpressionBuilder = builders.getOrPut(element) {
            val parentElement = element.elementParents.map { it.element }.firstOrNull { it in elements }
            ExpressionBuilder(element, parentElement?.let(::builderOf) ?: root)
        }
        elements.sortedBy { it.name }.forEach(::builderOf)
        for (builder in builders.values) {
            builder.parent?.takeUnless { it.isAbstract }?.isOpen = true
        }

        fun configure(element: Element, init: ExpressionBuilder.() -> Unit) = builders.getValue(element).init()

        configure(rootElement) {
            annotations += Expr("@IrExpressionBuilderDsl")
        }
        configure(expression) {
            initializationChecks += "type"
        }

        // Declaration references.

        configure(getValue) {
            initialValues["type"] = Expr("symbol.owner.type")
        }
        configure(setValue) {
            contextValues["type"] = unit()
        }
        configure(getField) {
            initialValues["type"] = Expr("symbol.owner.type")
        }
        configure(setField) {
            contextValues["type"] = unit()
        }
        configure(getObjectValue) {
            initialValues["type"] = Expr("symbol.owner.defaultType", listOf(defaultType))
        }
        configure(getEnumValue) {
            initialValues["type"] = Expr("symbol.owner.parentAsClass.defaultType", listOf(defaultType, parentAsClass))
        }

        // Calls and references: the arguments have the shape of the callee parameters, see `initializeShape`.

        configure(memberAccessExpression) {
            kDoc = """
                The arguments of a call have one slot per parameter of the callee, as [org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression.arguments]:
                the slots of a function call exist when the lambda runs, and are filled by index. `build` checks that their number wasn't changed.
            """.trimIndent()
            build.setByHook += listOf("arguments", "typeArguments")
            build.afterBuild = "initializeArguments"
        }
        for (functionAccess in listOf(call, constructorCall, delegatingConstructorCall, enumConstructorCall, functionReference)) {
            configure(functionAccess) {
                initHook = "initializeShape"
            }
        }
        configure(call) {
            initialValues["type"] = Expr("symbol.owner.returnType")
        }
        configure(constructorCall) {
            kDoc = "The type arguments are the ones of the constructed class followed by the ones of the constructor."
            defaults["constructorTypeArgumentsCount"] = Expr("0")
            initialValues["type"] = Expr("symbol.owner.returnType")
        }
        configure(annotation) {
            excludedFields += listOf("classSymbol", "argumentMapping") // Derived from the symbol and the arguments.
        }
        configure(delegatingConstructorCall) {
            contextValues["type"] = unit()
        }
        configure(functionReference) {
            defaults["reflectionTarget"] = Expr("symbol")
        }
        configure(instanceInitializerCall) {
            identityFields += "classSymbol"
            contextValues["type"] = unit()
        }

        // Blocks: `IrBuilder.irBlock {}` and `IrBuilder.irComposite {}` would be ambiguous with the old builders collecting statements.

        configure(block) {
            entryPoints -= IrBuilder
        }
        configure(composite) {
            entryPoints -= IrBuilder
        }
        configure(inlinedFunctionBlock) {
            defaults["inlinedFunctionStartOffset"] = Expr("UNDEFINED_OFFSET", listOf(undefinedOffset))
            defaults["inlinedFunctionEndOffset"] = Expr("UNDEFINED_OFFSET", listOf(undefinedOffset))
        }

        // Control flow.

        configure(`return`) {
            entryPoints -= IrBuilder
            entryPoints += IrBuilderWithScope
            contextValues["type"] = nothing()
            contextHook = "setReturnTargetFromScope"
        }
        configure(`throw`) {
            contextValues["type"] = nothing()
        }
        configure(breakContinue) {
            contextValues["type"] = nothing()
        }
        configure(loop) {
            contextValues["type"] = unit()
        }
        configure(elseBranch) {
            contextValues["condition"] = Expr(
                "IrConstImpl.boolean(startOffset, endOffset, context.irBuiltIns.booleanType, true)",
                listOf(importable("${Packages.exprs}.impl", "IrConstImpl"))
            )
        }
        configure(stringConcatenation) {
            contextValues["type"] = Expr("context.irBuiltIns.stringType")
        }

        // Constants: the kind is defined by the type of the value, so the entry points are overloads written by hand.

        configure(const) {
            entryPoints.clear()
        }
        configure(constantPrimitive) {
            kDoc = "Unless set explicitly, the type is the type of the value."
            build.beforeBuild = "applyDefaults"
        }

        // Bodies.

        configure(blockBody) {
            entryPoints.clear()
            entryPoints += IrFactory
            build.factoryMethod = "createBlockBody"
        }
        configure(expressionBody) {
            entryPoints.clear()
            entryPoints += IrFactory
            build.factoryMethod = "createExpressionBody"
        }
        configure(syntheticBody) {
            entryPoints -= IrBuilder
        }

        builders.values.toList()
    }

    /**
     * The properties declared in [builder] (not in its parents), in the order of the element fields.
     * Identity properties of a builder that isn't abstract are parameters of its constructor instead, see [constructorParameters].
     */
    fun properties(builder: ExpressionBuilder): List<ExpressionBuilderProperty> {
        val inherited = builder.parent?.let { inheritedFieldTypes(builder, it) } ?: emptyMap()
        return fields(builder).mapNotNull { field ->
            val isIdentity = isIdentity(builder, field)
            val inheritedType = inherited[field.name]
            when {
                isIdentity && !builder.isAbstract -> null
                inheritedType == null ->
                    ExpressionBuilderProperty(field, isIdentity, isOverride = false, default = default(builder, field, isIdentity))
                inheritedType == field.typeRef.toString() -> null
                isIdentity -> ExpressionBuilderProperty(field, isIdentity = true, isOverride = true, default = null)
                else -> error("${builder.typeName}: ${field.name} narrows the type of a mutable property of a parent builder")
            }
        }
    }

    /** The constructor parameters of a builder that isn't abstract: its identity properties. */
    fun constructorParameters(builder: ExpressionBuilder): List<ConstructorParameter> {
        check(!builder.isAbstract)
        val concreteParent = builder.parent?.takeUnless { it.isAbstract }
        val passedToParent = concreteParent?.let(::constructorParameters).orEmpty().mapTo(mutableSetOf()) { it.field.name }
        val inherited = builder.parent?.let { inheritedFieldTypes(builder, it) } ?: emptyMap()
        return fields(builder).filter { isIdentity(builder, it) }.map { field ->
            ConstructorParameter(field, isPassedToParent = field.name in passedToParent, isOverride = field.name in inherited)
        }
    }

    /** All properties of [builder] and its parents that are set on the node: the identity ones and the ones with values. */
    fun nodeProperties(builder: ExpressionBuilder): List<Field> = fields(builder)

    private fun fields(builder: ExpressionBuilder): List<Field> =
        builder.element.allFields.filter { it.name !in alwaysExcludedFields && it.name !in builder.excludedFields }

    private fun default(builder: ExpressionBuilder, field: Field, isIdentity: Boolean): Expr? =
        if (isIdentity) null else builder.defaults[field.name] ?: DeclarationBuilderConfigurator.defaultValue(field)

    private fun isIdentity(builder: ExpressionBuilder, field: Field): Boolean =
        (builder.ancestors + builder).any { field.name in it.identityFields } ||
                field.name == "symbol" && field.symbolFieldRole != null ||
                field is SimpleField && !field.isMutable

    /** Types of the fields of the parent builder's element, as seen from [builder] (type parameters substituted). */
    private fun inheritedFieldTypes(builder: ExpressionBuilder, parent: ExpressionBuilder): Map<String, String> {
        val args = builder.element.elementParents.singleOrNull { it.element == parent.element }?.args ?: emptyMap()
        return parent.element.allFields
            .filter { it.name !in alwaysExcludedFields && it.name !in parent.excludedFields }
            .associate { it.name to it.typeRef.substitute(args).toString() }
    }

    /** Whether all `lateinit` properties of [builder] get values when it's created by the [entryPoint], so the lambda may be omitted. */
    fun hasAllValues(builder: ExpressionBuilder, entryPoint: ExpressionEntryPoint): Boolean {
        val hierarchy = builder.ancestors + builder
        val assigned = hierarchy.flatMapTo(mutableSetOf()) { it.initialValues.keys }
        if (entryPoint == IrBuilder || entryPoint == IrBuilderWithScope) {
            hierarchy.flatMapTo(assigned) { it.contextValues.keys }
        }
        return hierarchy.flatMap { properties(it) }.none { !it.isIdentity && it.default == null && it.field.name !in assigned }
    }

    /** The [ExpressionBuilder.contextValues] of [builder] and its parents, the most specific ones win. */
    fun contextValues(builder: ExpressionBuilder): Map<String, Expr> =
        (builder.ancestors.asReversed() + builder).fold(linkedMapOf()) { values, it -> values.apply { putAll(it.contextValues) } }
}
