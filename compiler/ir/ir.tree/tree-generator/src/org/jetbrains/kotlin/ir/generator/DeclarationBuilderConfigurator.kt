/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator

import org.jetbrains.kotlin.generators.tree.AbstractField
import org.jetbrains.kotlin.generators.tree.ClassRef
import org.jetbrains.kotlin.generators.tree.ElementOrRef
import org.jetbrains.kotlin.generators.tree.StandardTypes
import org.jetbrains.kotlin.generators.tree.TypeRef
import org.jetbrains.kotlin.generators.tree.imports.ArbitraryImportable
import org.jetbrains.kotlin.generators.tree.type
import org.jetbrains.kotlin.ir.generator.model.*
import org.jetbrains.kotlin.ir.generator.model.ListField

/**
 * Configures the v2 declaration builders (`org.jetbrains.kotlin.ir.builders.declarations.v2`).
 *
 * The header properties of builders are derived from the fields of IR elements (see [headerProperties]):
 * - [alwaysExcludedFields], declared symbols and fields holding bodies are skipped;
 * - fields holding child elements are children, described in [DeclarationBuilder.children], or excluded explicitly
 *   (a child field that is neither is an error, so that a new child in the IR tree isn't silently ignored);
 * - defaults are derived from the field: see [defaultValue].
 *
 * The entry points are generated too (see [DeclarationBuilder.entryPoints]). What depends on the owner of a declaration (its parent,
 * defaults derived from the owner, the order of building the children) is hand-written: in the `@PublishedApi` functions the entry
 * points pass the builder to, and in the hooks called from the generated `build` through [BuildConfig.afterBuild].
 */
object DeclarationBuilderConfigurator {
    private val alwaysExcludedFields = setOf("descriptor", "factory", "attributeOwnerId")

    /**
     * Header properties that `copyFrom` doesn't copy: relations to other declarations, which a copy doesn't inherit,
     * and annotations, which are usually filtered and have to be deep-copied (`copyAnnotations()`).
     */
    private val notCopiedFields = setOf("annotations", "metadata", "overriddenSymbols", "sealedSubclasses", "companionExtensionClass")

    private val irPackage = Packages.tree

    private fun importable(packageName: String, name: String) = ArbitraryImportable(packageName, name)

    private val specialNames = importable("org.jetbrains.kotlin.name", "SpecialNames")
    private val parameterKind = importable(Packages.declarations, "IrParameterKind")
    private val nameType = importable("org.jetbrains.kotlin.name", "Name")
    private val irElement = importable(irPackage, "IrElement")
    private val irTypeParameterSymbol = importable(Packages.symbols, "IrTypeParameterSymbol")
    private val irValueParameterSymbol = importable(Packages.symbols, "IrValueParameterSymbol")
    private val irType = importable(Packages.types, "IrType")
    private val variance = importable("org.jetbrains.kotlin.types", "Variance")
    private val irDeclarationOrigin = importable(Packages.declarations, "IrDeclarationOrigin")
    private val descriptorVisibilities = importable(Packages.descriptors, "DescriptorVisibilities")

    private val irBuilderWithScopeType = type("org.jetbrains.kotlin.ir.builders", "IrBuilderWithScope")

    fun configure(): List<DeclarationBuilder> = with(IrTree) {
        val builders = mutableListOf<DeclarationBuilder>()
        fun builder(
            typeName: String,
            element: Element,
            parent: DeclarationBuilder?,
            isAbstract: Boolean = false,
            init: DeclarationBuilder.() -> Unit = {},
        ) = DeclarationBuilder(typeName, element, parent, isAbstract).apply(init).also { builders += it }

        val declarationBuilder = builder("IrDeclarationBuilder", declaration, parent = null, isAbstract = true) {
            annotations += Expr("@IrDeclarationBuilderDsl")
            // Annotations are created before the declaration, they don't depend on it.
            childFieldsAsProperties += "annotations"
            extraProperties += ExtraProperty(
                "parent",
                type(Packages.declarations, "IrDeclarationParent").copy(nullable = true),
                Expr("null"),
            )
            additionalMembers += Expr(
                """
                fun setSourceRange(from: IrElement) {
                    startOffset = from.startOffset
                    endOffset = from.endOffset
                }
                """.trimIndent(),
                listOf(irElement),
            )
        }

        val declarationWithNameBuilder = builder(
            "IrDeclarationWithNameBuilder", declarationWithName, declarationBuilder, isAbstract = true,
        ) {
            // The default names of property children are derived from the property, and are applied when it's built.
            initializationChecks += "name"
        }

        val typeParameterBuilder = builder("IrTypeParameterBuilder", typeParameter, declarationWithNameBuilder) {
            fileName = "IrTypeParameterBuilders"
            excludedFields += "index"
            // Supertypes may reference other type parameters of the same container, so they are remapped as a group.
            notCopiedFields += "superTypes"
            preallocatesSymbol = true
            build = BuildConfig(Creation.FactoryMethod("createTypeParameter")).apply {
                extraParameters += "index" to StandardTypes.int
            }
        }

        val typeParametersContainerBuilder = builder(
            "IrTypeParametersContainerBuilder", typeParametersContainer, declarationWithNameBuilder, isAbstract = true,
        ) {
            fileName = "IrTypeParameterBuilders"
            children += Child.Built(
                "typeParameters", isList = true, storedType = typeParameterBuilder,
                functions = listOf(ChildFunction("typeParameter", typeParameterBuilder)),
            )
            additionalMembers += Expr(
                """
                fun typeParameter(name: String, upperBound: IrType, variance: Variance = Variance.INVARIANT): IrTypeParameterSymbol =
                    typeParameter {
                        this.name = Name.identifier(name)
                        this.superTypes = [upperBound]
                        this.variance = variance
                    }
                """.trimIndent(),
                listOf(irType, variance, irTypeParameterSymbol, nameType),
            )
        }

        val parameterBuilder = builder("IrParameterBuilder", valueParameter, declarationWithNameBuilder, isAbstract = true) {
            fileName = "IrParameterBuilders"
            abstractFields += "kind"
            // The type of the class `thisReceiver` is derived from the class.
            initializationChecks += "type"
            preallocatesSymbol = true
            build = BuildConfig(Creation.FactoryMethod("createValueParameter"))
        }

        fun parameterKindBuilder(typeName: String, kind: String, isMutable: Boolean = false, init: DeclarationBuilder.() -> Unit = {}) =
            builder(typeName, valueParameter, parameterBuilder) {
                fileName = "IrParameterBuilders"
                fieldOverrides += FieldOverride("kind", isMutable, Expr("IrParameterKind.$kind", listOf(parameterKind)))
                init()
            }

        val valueParameterBuilder = parameterKindBuilder("IrValueParameterBuilder", "Regular", isMutable = true)
        val regularParameterBuilder = parameterKindBuilder("IrRegularParameterBuilder", "Regular")
        val dispatchReceiverBuilder = parameterKindBuilder("IrDispatchReceiverBuilder", "DispatchReceiver") {
            initialValues["name"] = Expr("SpecialNames.THIS", listOf(specialNames))
        }
        val extensionReceiverBuilder = parameterKindBuilder("IrExtensionReceiverBuilder", "ExtensionReceiver") {
            initialValues["name"] = Expr("SpecialNames.THIS", listOf(specialNames))
        }
        val contextParameterBuilder = parameterKindBuilder("IrContextParameterBuilder", "Context")

        val functionBuilder = builder("IrFunctionBuilder", function, typeParametersContainerBuilder, isAbstract = true) {
            nullableInBuilder += "returnType"
            // `IrFunction.parameters` is declared by hand in IrFunction, not in the model.
            children += Child.Built(
                "parameters", isList = true, storedType = parameterBuilder,
                functions = listOf(
                    ChildFunction("dispatchReceiver", dispatchReceiverBuilder),
                    ChildFunction("contextParameter", contextParameterBuilder),
                    ChildFunction("extensionReceiver", extensionReceiverBuilder),
                    ChildFunction("valueParameter", regularParameterBuilder),
                    ChildFunction("parameter", valueParameterBuilder),
                ),
            )
            additionalMembers += Expr(
                """
                fun valueParameter(name: Name, type: IrType): IrValueParameterSymbol =
                    valueParameter {
                        this.name = name
                        this.type = type
                    }

                fun valueParameter(name: String, type: IrType): IrValueParameterSymbol =
                    valueParameter(Name.identifier(name), type)
                """.trimIndent(),
                listOf(nameType, irType, irValueParameterSymbol),
            )
        }

        val simpleFunctionBuilder = builder("IrSimpleFunctionBuilder", simpleFunction, functionBuilder) {
            excludedFields += "correspondingPropertySymbol" // Set by the property.
            preallocatesSymbol = true
            build = BuildConfig(Creation.FactoryMethod("createSimpleFunction")).apply {
                extraArguments += "returnType"
                afterBuild = "buildChildren"
            }
            entryPoints += DeclarationEntryPoint(irFactoryType, "irFunction", "build")
            entryPoints += DeclarationEntryPoint(
                `class`, "irFunction", "buildMemberFunction",
                kDoc = "A member function. Unless the builder declares a dispatch receiver explicitly, it gets a copy of the class [IrClass.thisReceiver].",
            )
            entryPoints += DeclarationEntryPoint(`class`, "irStaticFunction", "buildStaticFunction")
        }

        builder("IrConstructorBuilder", constructor, functionBuilder) {
            initialValues["name"] = Expr("SpecialNames.INIT", listOf(specialNames))
            build = BuildConfig(Creation.FactoryMethod("createConstructor")).apply {
                extraArguments += "returnType"
                afterBuild = "buildChildren"
            }
            entryPoints += DeclarationEntryPoint(
                `class`, "irConstructor", "buildMemberConstructor", hasDefaultLambda = true,
                kDoc = "Unless set explicitly, the return type is the class type ([IrClass.defaultType]).",
            )
        }

        builder("IrClassBuilder", `class`, typeParametersContainerBuilder) {
            children += Child.Built(
                "thisReceiver", isList = false, storedType = dispatchReceiverBuilder,
                functions = listOf(
                    ChildFunction(
                        "thisReceiver", dispatchReceiverBuilder,
                        kDoc = """
                            Unless set explicitly, offsets, origin ([org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.INSTANCE_RECEIVER])
                            and type (the class type with its type parameters) are taken from the class when it's built.
                        """.trimIndent(),
                        hasDefaultLambda = true,
                        returnsSymbol = false,
                    ),
                ),
            )
            children += Child.Nodes("declarations", declaration, "declaration")
            build = BuildConfig(Creation.FactoryMethod("createClass")).apply {
                afterBuild = "buildChildren"
            }
            entryPoints += DeclarationEntryPoint(irFactoryType, "irClass", "build")
        }

        // The defaults of a backing field, the same for `IrPropertyBuilder.backingField {}` and `IrProperty.irBackingField {}`.
        val backingFieldCreator = BuilderCreator(
            "backingFieldBuilder",
            values = linkedMapOf(
                "origin" to Expr("IrDeclarationOrigin.PROPERTY_BACKING_FIELD", listOf(irDeclarationOrigin)),
                "visibility" to Expr("DescriptorVisibilities.PRIVATE", listOf(descriptorVisibilities)),
            ),
        )

        val fieldBuilder = builder("IrFieldBuilder", field, declarationWithNameBuilder) {
            excludedFields += "correspondingPropertySymbol" // Set by the property.
            preallocatesSymbol = true
            build = BuildConfig(Creation.FactoryMethod("createField"))
            creators += backingFieldCreator
            entryPoints += DeclarationEntryPoint(irFactoryType, "irField", "build")
            entryPoints += DeclarationEntryPoint(`class`, "irField", "buildMemberField")
        }

        builder("IrPropertyBuilder", property, declarationWithNameBuilder) {
            children += Child.Built(
                "backingField", isList = false, storedType = fieldBuilder,
                functions = listOf(
                    ChildFunction(
                        "backingField", fieldBuilder,
                        kDoc = "Unless set explicitly, the name is the name of the property.",
                        creator = backingFieldCreator,
                        alreadyDeclaredMessage = "The backing field is already declared",
                    ),
                ),
            )
            children += Child.Built(
                "getter", isList = false, storedType = simpleFunctionBuilder,
                functions = listOf(
                    ChildFunction(
                        "getter", simpleFunctionBuilder,
                        kDoc = "Unless set explicitly, the name is `<get-name>`.",
                        alreadyDeclaredMessage = "The getter is already declared",
                    ),
                ),
            )
            children += Child.Built(
                "setter", isList = false, storedType = simpleFunctionBuilder,
                functions = listOf(
                    ChildFunction(
                        "setter", simpleFunctionBuilder,
                        kDoc = "Unless set explicitly, the name is `<set-name>`. The value parameter isn't added implicitly.",
                        alreadyDeclaredMessage = "The setter is already declared",
                    ),
                ),
            )
            build = BuildConfig(Creation.FactoryMethod("createProperty")).apply {
                afterBuild = "buildChildren"
            }
            entryPoints += DeclarationEntryPoint(irFactoryType, "irProperty", "build")
            entryPoints += DeclarationEntryPoint(
                `class`, "irProperty", "buildMemberProperty",
                kDoc = "A member property: its accessors get dispatch receivers the same way as [irFunction] does.",
            )
            entryPoints += DeclarationEntryPoint(
                `class`, "irStaticProperty", "buildStaticProperty",
                kDoc = "A property with accessors without dispatch receivers, see [irStaticFunction]. The backing field must be marked static explicitly.",
            )
        }

        // Children of an existing property, e.g. one declared by a frontend plugin, whose IR parts are generated later.
        fieldBuilder.entryPoints += DeclarationEntryPoint(
            property, "irBackingField", "buildBackingField", creator = backingFieldCreator,
        )
        simpleFunctionBuilder.entryPoints += DeclarationEntryPoint(
            property, "irGetter", "buildGetter",
        )
        simpleFunctionBuilder.entryPoints += DeclarationEntryPoint(
            property, "irSetter", "buildSetter",
        )

        builder("IrVariableBuilder", variable, declarationWithNameBuilder) {
            kDoc = "Unless set explicitly, the type is the type of the initializer."
            // An expression that doesn't depend on the variable, unlike bodies of other declarations.
            childFieldsAsProperties += "initializer"
            initializationChecks += "type"
            build = BuildConfig(Creation.Function(importable("${Packages.declarations}.impl", "IrVariableImpl"))).apply {
                beforeBuild = "applyDefaults"
            }
            entryPoints += DeclarationEntryPoint(irFactoryType, "irVariable", "build")
            val localVariableCreator = BuilderCreator("localVariableBuilder", irBuilderWithScopeType, offsetsFromReceiver = true)
            creators += localVariableCreator
            entryPoints += DeclarationEntryPoint(
                irBuilderWithScopeType, "irVariable", "buildLocalVariable", creator = localVariableCreator,
            )
        }

        builder("IrAnonymousInitializerBuilder", anonymousInitializer, declarationBuilder) {
            build = BuildConfig(Creation.FactoryMethod("createAnonymousInitializer"))
            entryPoints += DeclarationEntryPoint(
                `class`, "irAnonymousInitializer", "buildMemberAnonymousInitializer", hasDefaultLambda = true,
            )
        }

        builders
    }

    /** The header properties declared in [builder] (not in its parents), in the order of the element fields. */
    fun headerProperties(builder: DeclarationBuilder): List<HeaderProperty> {
        val inherited = builder.ancestors.flatMapTo(mutableSetOf()) { ancestor -> ancestor.element.allFields.map { it.name } }
        val childNames = builder.children.mapTo(mutableSetOf()) { it.propertyName }
        return builder.element.allFields
            .filter { it.name !in inherited }
            .filter { field ->
                when {
                    field.name in alwaysExcludedFields -> false
                    field.symbolFieldRole == AbstractField.SymbolFieldRole.DECLARED -> false
                    field.holdsBody() -> false
                    field.name in builder.excludedFields -> false
                    field.name in childNames -> false
                    field.holdsChildElement() && field.name !in builder.childFieldsAsProperties ->
                        error(
                            "${builder.typeName}: the child field ${builder.element.name}.${field.name} " +
                                    "must be either a child of the builder or excluded"
                        )
                    else -> true
                }
            }
            .map { field ->
                val isAbstract = field.name in builder.abstractFields
                val nullable = field.name in builder.nullableInBuilder
                val type = if (nullable) field.typeRef.copy(nullable = true) else field.typeRef
                HeaderProperty(
                    field = field,
                    type = type,
                    default = if (isAbstract) null else if (nullable) Expr("null") else defaultValue(field),
                    isAbstract = isAbstract,
                )
            }
    }

    /** All header properties of [builder] and its parents that are fields of its element, i.e. that are set on the built node. */
    fun headerPropertiesOfNode(builder: DeclarationBuilder): List<HeaderProperty> {
        val elementFields = builder.element.allFields.mapTo(mutableSetOf()) { it.name }
        return (builder.ancestors.asReversed() + builder)
            .flatMap { headerProperties(it) }
            .filter { it.field.name in elementFields }
    }

    /**
     * The properties `copyFrom` of [builder] copies: all header properties of its node, except the abstract ones (fixed by the builder type,
     * e.g. the kind of a parameter), child elements (e.g. the initializer of a variable) and the [notCopiedFields].
     * Empty if [builder] adds nothing to the `copyFrom` of its parent.
     */
    fun copiedProperties(builder: DeclarationBuilder): List<HeaderProperty> {
        val notCopied = (builder.ancestors + builder).flatMapTo(notCopiedFields.toMutableSet()) { it.notCopiedFields }
        fun isCopied(property: HeaderProperty) =
            !property.isAbstract && !property.field.holdsChildElement() && property.field.name !in notCopied
        if (headerProperties(builder).none(::isCopied)) return emptyList()
        return headerPropertiesOfNode(builder).filter(::isCopied)
    }

    /** The default of a builder property derived from the field, `null` if it has none (the property is `lateinit`). */
    internal fun defaultValue(field: Field): Expr? {
        val className = (field.typeRef as? ClassRef<*>)?.typeName
        return when {
            field.name == "startOffset" || field.name == "endOffset" ->
                Expr("UNDEFINED_OFFSET", listOf(importable(irPackage, "UNDEFINED_OFFSET")))
            className == "IrDeclarationOrigin" -> Expr("IrDeclarationOrigin.DEFINED")
            field is ListField -> Expr("[]")
            field.nullable -> Expr("null")
            className == "Boolean" -> Expr("false")
            className == "DescriptorVisibility" ->
                Expr("DescriptorVisibilities.PUBLIC", listOf(importable(Packages.descriptors, "DescriptorVisibilities")))
            className == "Modality" -> Expr("Modality.FINAL")
            className == "ClassKind" -> Expr("ClassKind.CLASS")
            className == "Variance" -> Expr("Variance.INVARIANT")
            className == "SourceElement" -> Expr("SourceElement.NO_SOURCE")
            else -> null
        }
    }

    private fun Field.elementType(): Element? {
        val type: TypeRef = if (this is ListField) baseType else typeRef
        return (type as? ElementOrRef<*>)?.element as? Element
    }

    private fun Field.holdsChildElement(): Boolean = isChild && elementType() != null

    private fun Field.holdsBody(): Boolean {
        val element = elementType() ?: return false
        fun isBody(e: Element): Boolean = e == IrTree.body || e.allParents.any(::isBody)
        return isBody(element)
    }
}
