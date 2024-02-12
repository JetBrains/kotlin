/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.ValueClassRepresentation
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.FunctionParameter
import org.jetbrains.kotlin.generators.tree.printer.printFunctionDeclaration
import org.jetbrains.kotlin.generators.tree.printer.printFunctionWithBlockBody
import org.jetbrains.kotlin.ir.generator.config.AbstractTreeBuilder
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Element.Category.*
import org.jetbrains.kotlin.ir.generator.model.ListField.Mutability.*
import org.jetbrains.kotlin.ir.generator.model.ListField.Mutability.Array
import org.jetbrains.kotlin.ir.generator.model.ListField.Mutability.MutableList
import org.jetbrains.kotlin.ir.generator.model.SingleField
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.withIndent

// Note the style of the DSL to describe IR elements, which is these things in the following order:
// 1) config (see properties of Element)
// 2) parents
// 3) fields
object IrTree : AbstractTreeBuilder() {
    private fun symbol(type: TypeRefWithNullability, mutable: Boolean = false): SingleField =
        field("symbol", type, mutable = mutable)

    private fun descriptor(typeName: String, nullable: Boolean = false): SingleField =
        field(
            name = "descriptor",
            type = type(Packages.descriptors, typeName),
            mutable = false,
            nullable = nullable,
        ) {
            optInAnnotation = obsoleteDescriptorBasedApiAnnotation
            skipInIrFactory()
        }

    private fun declarationWithLateBinding(symbol: ClassRef<*>, initializer: Element.() -> Unit) = element(Declaration) {
        initializer()

        noAcceptMethod()
        noMethodInVisitor()

        fieldsToSkipInIrFactoryMethod.add("symbol")
        fieldsToSkipInIrFactoryMethod.add("containerSource")

        +field("isBound", boolean, mutable = false) {
            skipInIrFactory()
        }

        generationCallback = {
            println()
            printFunctionDeclaration(
                name = "acquireSymbol",
                parameters = listOf(FunctionParameter("symbol", symbol)),
                returnType = this@element,
                modality = Modality.ABSTRACT,
            )
            println()
        }
    }

    private val factory: SingleField = field("factory", irFactoryType, mutable = false) {
        skipInIrFactory()
    }

    override val rootElement: Element by element(Other, name = "Element") {
        needAcceptMethod()
        needTransformMethod()
        transformByChildren = true

        fun offsetField(prefix: String) = field(prefix + "Offset", int, mutable = false) {
            kDoc = """
            The $prefix offset of the syntax node from which this IR node was generated,
            in number of characters from the start of the source file. If there is no source information for this IR node,
            the [UNDEFINED_OFFSET] constant is used. In order to get the line number and the column number from this offset,
            [IrFileEntry.getLineNumber] and [IrFileEntry.getColumnNumber] can be used.
            
            @see IrFileEntry.getSourceRangeInfo
            """.trimIndent()
        }

        +offsetField("start")
        +offsetField("end")

        kDoc = "The root interface of the IR tree. Each IR node implements this interface."
    }
    val statement: Element by element(Other)

    val declaration: Element by element(Declaration) {
        parent(statement)
        parent(symbolOwner)
        parent(mutableAnnotationContainerType)

        +descriptor("DeclarationDescriptor")
        +field("origin", type(Packages.declarations, "IrDeclarationOrigin"))
        +field("parent", declarationParent, isChild = false) {
            skipInIrFactory()
        }
        +factory
    }
    val declarationBase: Element by element(Declaration) {
        typeKind = TypeKind.Class
        transformByChildren = true
        transformerReturnType = statement
        nameInVisitorMethod = "Declaration"

        parent(declaration)
    }
    val declarationParent: Element by element(Declaration)
    val declarationWithVisibility: Element by element(Declaration) {
        parent(declaration)

        +field("visibility", type(Packages.descriptors, "DescriptorVisibility"))
    }
    val declarationWithName: Element by element(Declaration) {
        parent(declaration)

        +field("name", type<Name>())
    }
    val possiblyExternalDeclaration: Element by element(Declaration) {
        parent(declarationWithName)

        +field("isExternal", boolean) {
            useFieldInIrFactory(defaultValue = "false")
        }
    }
    val symbolOwner: Element by element(Declaration) {
        +symbol(symbolType)
    }
    val metadataSourceOwner: Element by element(Declaration) {
        val metadataField = +field("metadata", type(Packages.declarations, "MetadataSource"), nullable = true) {
            skipInIrFactory()
            kDoc = """
            The arbitrary metadata associated with this IR node.
            
            @see $typeName
            """.trimIndent()
        }
        kDoc = """
        An [${rootElement.typeName}] capable of holding something which backends can use to write
        as the metadata for the declaration.
        
        Technically, it can even be ± an array of bytes, but right now it's usually the frontend representation of the declaration,
        so a descriptor in case of K1, and [org.jetbrains.kotlin.fir.FirElement] in case of K2,
        and the backend invokes a metadata serializer on it to obtain metadata and write it, for example, to `@kotlin.Metadata`
        on JVM.
        
        In Kotlin/Native, [${metadataField.name}] is used to store some LLVM-related stuff in an IR declaration,
        but this is only for performance purposes (before it was done using simple maps).
        """.trimIndent()
    }
    val overridableMember: Element by element(Declaration) {
        parent(declaration)
        parent(declarationWithVisibility)
        parent(declarationWithName)
        parent(symbolOwner)

        +field("modality", type<Modality>())
    }
    val overridableDeclaration: Element by element(Declaration) {
        val s = +param("S", symbolType)

        parent(overridableMember)

        +field("symbol", s, mutable = false)
        +isFakeOverrideField()
        +listField("overriddenSymbols", s, mutability = Var) {
            skipInIrFactory()
        }
    }
    val memberWithContainerSource: Element by element(Declaration) {
        parent(declarationWithName)

        +field("containerSource", type<DeserializedContainerSource>(), nullable = true, mutable = false) {
            useFieldInIrFactory(defaultValue = "null")
        }
    }
    val valueDeclaration: Element by element(Declaration) {
        parent(declarationWithName)
        parent(symbolOwner)

        +descriptor("ValueDescriptor")
        +symbol(valueSymbolType)
        +field("type", irTypeType)
        +field("isAssignable", boolean, mutable = false)
    }
    val valueParameter: Element by element(Declaration) {
        needTransformMethod()

        parent(declarationBase)
        parent(valueDeclaration)

        +descriptor("ParameterDescriptor")
        +symbol(valueParameterSymbolType)
        +field("index", int)
        +field("varargElementType", irTypeType, nullable = true)
        +field("isCrossinline", boolean)
        +field("isNoinline", boolean)
        +field("isHidden", boolean) {
            additionalImports.add(idSignatureType)
            kDoc = """
            If `true`, the value parameter does not participate in [IdSignature] computation.

            This is a workaround that is needed for better support of compiler plugins.
            Suppose you have the following code and some IR plugin that adds a value parameter to functions
            marked with the `@PluginMarker` annotation.
            ```kotlin
            @PluginMarker
            fun foo(defined: Int) { /* ... */ }
            ```

            Suppose that after applying the plugin the function is changed to:
            ```kotlin
            @PluginMarker
            fun foo(defined: Int, ${'$'}extra: String) { /* ... */ }
            ```

            If a compiler plugin adds parameters to an [${function.typeName}],
            the representations of the function in the frontend and in the backend may diverge, potentially causing signature mismatch and
            linkage errors (see [KT-40980](https://youtrack.jetbrains.com/issue/KT-40980)).
            We wouldn't want IR plugins to affect the frontend representation, since in an IDE you'd want to be able to see those
            declarations in their original form (without the `${'$'}extra` parameter).

            To fix this problem, [$name] was introduced.
            
            TODO: consider dropping [$name] if it isn't used by any known plugin.
            """.trimIndent()
        }
        +field("defaultValue", expressionBody, nullable = true)
    }
    val `class`: Element by element(Declaration) {
        parent(declarationBase)
        parent(possiblyExternalDeclaration)
        parent(declarationWithVisibility)
        parent(typeParametersContainer)
        parent(declarationContainer)
        parent(attributeContainer)
        parent(metadataSourceOwner)

        +descriptor("ClassDescriptor")
        +symbol(classSymbolType)
        +field("kind", type<ClassKind>())
        +field("modality", type<Modality>())
        +field("isCompanion", boolean) {
            useFieldInIrFactory(defaultValue = "false")
        }
        +field("isInner", boolean) {
            useFieldInIrFactory(defaultValue = "false")
        }
        +field("isData", boolean) {
            useFieldInIrFactory(defaultValue = "false")
        }
        +field("isValue", boolean) {
            useFieldInIrFactory(defaultValue = "false")
        }
        +field("isExpect", boolean) {
            useFieldInIrFactory(defaultValue = "false")
        }
        +field("isFun", boolean) {
            useFieldInIrFactory(defaultValue = "false")
        }
        +field("hasEnumEntries", boolean) {
            useFieldInIrFactory(defaultValue = "false")
            kDoc = """
            Returns true iff this is a class loaded from dependencies which has the `HAS_ENUM_ENTRIES` metadata flag set.
            This flag is useful for Kotlin/JVM to determine whether an enum class from dependency actually has the `entries` property
            in its bytecode, as opposed to whether it has it in its member scope, which is true even for enum classes compiled by
            old versions of Kotlin which did not support the EnumEntries language feature.
            """.trimIndent()
        }
        +field("source", type<SourceElement>(), mutable = false) {
            useFieldInIrFactory(defaultValue = "SourceElement.NO_SOURCE")
        }
        +listField("superTypes", irTypeType, mutability = Var) {
            skipInIrFactory()
        }
        +field("thisReceiver", valueParameter, nullable = true)
        +field(
            "valueClassRepresentation",
            type<ValueClassRepresentation<*>>().withArgs(type(Packages.types, "IrSimpleType")),
            nullable = true,
        ) {
            skipInIrFactory()
        }
        +listField("sealedSubclasses", classSymbolType, mutability = Var) {
            skipInIrFactory()
            kDoc = """
            If this is a sealed class or interface, this list contains symbols of all its immediate subclasses.
            Otherwise, this is an empty list.
            
            NOTE: If this [$typeName] was deserialized from a klib, this list will always be empty!
            See [KT-54028](https://youtrack.jetbrains.com/issue/KT-54028).
            """.trimIndent()
        }
    }
    val attributeContainer: Element by element(Declaration) {
        kDoc = """
            Represents an IR element that can be copied, but must remember its original element. It is
            useful, for example, to keep track of generated names for anonymous declarations.
            @property attributeOwnerId original element before copying. Always satisfies the following
              invariant: `this.attributeOwnerId == this.attributeOwnerId.attributeOwnerId`.
            @property originalBeforeInline original element before inlining. Useful only with IR
              inliner. `null` if the element wasn't inlined. Unlike [attributeOwnerId], doesn't have the
              idempotence invariant and can contain a chain of declarations.
        """.trimIndent()

        +field("attributeOwnerId", attributeContainer, isChild = false) {
            skipInIrFactory()
        }
        // null <=> this element wasn't inlined
        +field("originalBeforeInline", attributeContainer, nullable = true, isChild = false) {
            skipInIrFactory()
        }
    }
    val anonymousInitializer: Element by element(Declaration) {
        parent(declarationBase)

        kDoc = """
        Represents a single `init {}` block in a Kotlin class.
        """.trimIndent()

        +descriptor("ClassDescriptor") // TODO special descriptor for anonymous initializer blocks
        +symbol(anonymousInitializerSymbolType)
        +field("isStatic", boolean) {
            useFieldInIrFactory(defaultValue = "false")
        }
        +field("body", blockBody)
    }
    val declarationContainer: Element by element(Declaration) {
        ownsChildren = false

        parent(declarationParent)

        +listField("declarations", declaration, mutability = MutableList) {
            kDoc = """
                 Accessing list of declaration may trigger lazy declaration list computation for lazy class,
                   which requires computation of fake-overrides for this class. So it's unsafe to access it
                   before IR for all sources is built (because fake-overrides of lazy classes may depend on
                   declaration of source classes, e.g. for java source classes)
            """.trimIndent()
            optInAnnotation = unsafeDuringIrConstructionApiAnnotation
        }
    }
    val typeParametersContainer: Element by element(Declaration) {
        ownsChildren = false

        parent(declaration)
        parent(declarationParent)

        +listField("typeParameters", typeParameter, mutability = Var)
    }
    val typeParameter: Element by element(Declaration) {
        needTransformMethod()

        parent(declarationBase)
        parent(declarationWithName)

        +descriptor("TypeParameterDescriptor")
        +symbol(typeParameterSymbolType)
        +field("variance", type<Variance>())
        +field("index", int)
        +field("isReified", boolean)
        +listField("superTypes", irTypeType, mutability = Var) {
            skipInIrFactory()
        }
    }
    val returnTarget: Element by element(Declaration) {
        parent(symbolOwner)

        +descriptor("FunctionDescriptor")
        +symbol(returnTargetSymbolType)
    }
    val function: Element by element(Declaration) {
        parent(declarationBase)
        parent(possiblyExternalDeclaration)
        parent(declarationWithVisibility)
        parent(typeParametersContainer)
        parent(symbolOwner)
        parent(declarationParent)
        parent(returnTarget)
        parent(memberWithContainerSource)
        parent(metadataSourceOwner)

        +descriptor("FunctionDescriptor")
        +symbol(functionSymbolType)
        // NB: there's an inline constructor for Array and each primitive array class.
        +field("isInline", boolean)
        +field("isExpect", boolean)
        +field("returnType", irTypeType)
        +field("dispatchReceiverParameter", valueParameter, nullable = true)
        +field("extensionReceiverParameter", valueParameter, nullable = true)
        +listField("valueParameters", valueParameter, mutability = Var)
        // The first `contextReceiverParametersCount` value parameters are context receivers.
        +field("contextReceiverParametersCount", int) {
            skipInIrFactory()
        }
        +field("body", body, nullable = true)
    }
    val constructor: Element by element(Declaration) {
        parent(function)

        +descriptor("ClassConstructorDescriptor")
        +symbol(constructorSymbolType)
        +field("isPrimary", boolean)
    }
    val enumEntry: Element by element(Declaration) {
        parent(declarationBase)
        parent(declarationWithName)

        +descriptor("ClassDescriptor")
        +symbol(enumEntrySymbolType)
        +field("initializerExpression", expressionBody, nullable = true)
        +field("correspondingClass", `class`, nullable = true)
    }
    val errorDeclaration: Element by element(Declaration) {
        parent(declarationBase)

        additionalIrFactoryMethodParameters.add(
            descriptor("DeclarationDescriptor", nullable = true).apply {
                useFieldInIrFactory(defaultValue = "null")
            }
        )

        fieldsToSkipInIrFactoryMethod.add("origin")

        +field("symbol", symbolType, mutable = false) {
            baseGetter = "error(\"Should never be called\")"
            skipInIrFactory()
        }
    }
    val functionWithLateBinding: Element by declarationWithLateBinding(simpleFunctionSymbolType) {
        parent(simpleFunction)
    }
    val propertyWithLateBinding: Element by declarationWithLateBinding(propertySymbolType) {
        parent(property)
    }
    val field: Element by element(Declaration) {
        parent(declarationBase)
        parent(possiblyExternalDeclaration)
        parent(declarationWithVisibility)
        parent(declarationParent)
        parent(metadataSourceOwner)

        +descriptor("PropertyDescriptor")
        +symbol(fieldSymbolType)
        +field("type", irTypeType)
        +field("isFinal", boolean)
        +field("isStatic", boolean)
        +field("initializer", expressionBody, nullable = true)
        +field("correspondingPropertySymbol", propertySymbolType, nullable = true) {
            skipInIrFactory()
        }
    }
    val localDelegatedProperty: Element by element(Declaration) {
        parent(declarationBase)
        parent(declarationWithName)
        parent(symbolOwner)
        parent(metadataSourceOwner)

        +descriptor("VariableDescriptorWithAccessors")
        +symbol(localDelegatedPropertySymbolType)
        +field("type", irTypeType)
        +field("isVar", boolean)
        +field("delegate", variable)
        +field("getter", simpleFunction)
        +field("setter", simpleFunction, nullable = true)
    }
    val moduleFragment: Element by element(Declaration) {
        needTransformMethod()
        transformByChildren = true
        generateIrFactoryMethod = false

        +descriptor("ModuleDescriptor").apply {
            optInAnnotation = null
        }
        +field("name", type<Name>(), mutable = false)
        +field("irBuiltins", type(Packages.tree, "IrBuiltIns"), mutable = false)
        +listField("files", file, mutability = MutableList)
        additionalImports += ArbitraryImportable(Packages.tree, "UNDEFINED_OFFSET")
        +field("startOffset", int, mutable = false) {
            baseGetter = "UNDEFINED_OFFSET"
        }
        +field("endOffset", int, mutable = false) {
            baseGetter = "UNDEFINED_OFFSET"
        }
    }
    val property: Element by element(Declaration) {
        isLeaf = true

        parent(declarationBase)
        parent(possiblyExternalDeclaration)
        parent(overridableDeclaration.withArgs("S" to propertySymbolType))
        parent(metadataSourceOwner)
        parent(attributeContainer)
        parent(memberWithContainerSource)

        +descriptor("PropertyDescriptor")
        +symbol(propertySymbolType)
        +field("isVar", boolean)
        +field("isConst", boolean)
        +field("isLateinit", boolean)
        +field("isDelegated", boolean)
        +field("isExpect", boolean) {
            useFieldInIrFactory(defaultValue = "false")
        }
        +isFakeOverrideField()
        +field("backingField", field, nullable = true)
        +field("getter", simpleFunction, nullable = true)
        +field("setter", simpleFunction, nullable = true)
    }

    private fun isFakeOverrideField() = field("isFakeOverride", boolean) {
        useFieldInIrFactory(defaultValue = "origin == IrDeclarationOrigin.FAKE_OVERRIDE")
    }

    //TODO: make IrScript as IrPackageFragment, because script is used as a file, not as a class
    //NOTE: declarations and statements stored separately
    val script: Element by element(Declaration) {
        generateIrFactoryMethod = false

        parent(declarationBase)
        parent(declarationWithName)
        parent(declarationParent)
        parent(statementContainer)
        parent(metadataSourceOwner)

        +symbol(scriptSymbolType)
        // NOTE: is the result of the FE conversion, because there script interpreted as a class and has receiver
        // TODO: consider removing from here and handle appropriately in the lowering
        +field("thisReceiver", valueParameter, nullable = true) // K1
        +field("baseClass", irTypeType, nullable = true) // K1
        +listField("explicitCallParameters", variable, mutability = Var)
        +listField("implicitReceiversParameters", valueParameter, mutability = Var)
        +listField("providedProperties", propertySymbolType, mutability = Var)
        +listField("providedPropertiesParameters", valueParameter, mutability = Var)
        +field("resultProperty", propertySymbolType, nullable = true)
        +field("earlierScriptsParameter", valueParameter, nullable = true)
        +listField("importedScripts", scriptSymbolType, mutability = Var, nullable = true)
        +listField("earlierScripts", scriptSymbolType, mutability = Var, nullable = true)
        +field("targetClass", classSymbolType, nullable = true)
        +field("constructor", constructor, nullable = true, isChild = false) // K1
    }
    val simpleFunction: Element by element(Declaration) {
        isLeaf = true

        parent(function)
        parent(overridableDeclaration.withArgs("S" to simpleFunctionSymbolType))
        parent(attributeContainer)

        +symbol(simpleFunctionSymbolType)
        +field("isTailrec", boolean)
        +field("isSuspend", boolean)
        +isFakeOverrideField()
        +field("isOperator", boolean)
        +field("isInfix", boolean)
        +field("correspondingPropertySymbol", propertySymbolType, nullable = true) {
            skipInIrFactory()
        }
    }
    val typeAlias: Element by element(Declaration) {
        parent(declarationBase)
        parent(declarationWithName)
        parent(declarationWithVisibility)
        parent(typeParametersContainer)

        +descriptor("TypeAliasDescriptor")
        +symbol(typeAliasSymbolType)
        +field("isActual", boolean)
        +field("expandedType", irTypeType)
    }
    val variable: Element by element(Declaration) {
        generateIrFactoryMethod = false

        parent(declarationBase)
        parent(valueDeclaration)

        +descriptor("VariableDescriptor")
        +symbol(variableSymbolType)
        +field("isVar", boolean)
        +field("isConst", boolean)
        +field("isLateinit", boolean)
        +field("initializer", expression, nullable = true)
    }
    val packageFragment: Element by element(Declaration) {
        ownsChildren = false

        parent(declarationContainer)
        parent(symbolOwner)

        +symbol(packageFragmentSymbolType)
        +field("packageFragmentDescriptor", type(Packages.descriptors, "PackageFragmentDescriptor"), mutable = false) {
            optInAnnotation = obsoleteDescriptorBasedApiAnnotation
        }
        +field("moduleDescriptor", type(Packages.descriptors, "ModuleDescriptor"), mutable = false) {
            kDoc = """
            This should be a link to [IrModuleFragment] instead. 
               
            Unfortunately, some package fragments (e.g. some synthetic ones and [IrExternalPackageFragment])
            are not located in any IR module, but still have a module descriptor. 
            """.trimIndent()
        }
        +field("packageFqName", type<FqName>())
        +field("fqName", type<FqName>()) {
            baseGetter = "packageFqName"
            customSetter = "packageFqName = value"
            deprecation = Deprecated(
                "Please use `packageFqName` instead",
                ReplaceWith("packageFqName"),
                DeprecationLevel.ERROR,
            )
        }
    }
    val externalPackageFragment: Element by element(Declaration) {
        transformByChildren = true
        generateIrFactoryMethod = false

        kDoc = """
            This is a root parent element for external declarations (meaning those that come from
            another compilation unit/module, not to be confused with [IrPossiblyExternalDeclaration.isExternal]). 
            
            Each declaration is contained either in some [IrFile], or in some [IrExternalPackageFragment].
            Declarations coming from dependencies are located in [IrExternalPackageFragment].
            
            It can be used for obtaining a module descriptor, which contains the information about
            the module from which the declaration came. It would be more correct to have a link to some
            [IrModuleFragment] instead, which would make [IrModuleFragment] the only source of truth about modules,
            but this is how things are now.
            
            Also, it can be used for checking whether some declaration is external (by checking whether its top
            level parent is an [IrExternalPackageFragment]). But it is not possible
            to get all declarations from a fragment. Also, being in the same or different
            fragment doesn't mean anything. There can be more than one fragment for the same dependency.
        """.trimIndent()

        parent(packageFragment)

        +symbol(externalPackageFragmentSymbolType)
        +field("containerSource", type<DeserializedContainerSource>(), nullable = true, mutable = false)
    }
    val file: Element by element(Declaration) {
        needTransformMethod()
        transformByChildren = true
        generateIrFactoryMethod = false

        parent(packageFragment)
        parent(mutableAnnotationContainerType)
        parent(metadataSourceOwner)

        +symbol(fileSymbolType)
        +field("module", moduleFragment, isChild = false)
        +field("fileEntry", type(Packages.tree, "IrFileEntry"))
    }

    val expression: Element by element(Expression) {
        needTransformMethod()
        transformByChildren = true

        parent(statement)
        parent(varargElement)
        parent(attributeContainer)

        +field("attributeOwnerId", attributeContainer, isChild = false) {
            baseDefaultValue = "this"
            skipInIrFactory()
        }
        +field("originalBeforeInline", attributeContainer, nullable = true, isChild = false) {
            baseDefaultValue = "null"
            skipInIrFactory()
        }
        +field("type", irTypeType)
    }
    val statementContainer: Element by element(Expression) {
        ownsChildren = false

        +listField("statements", statement, mutability = MutableList)
    }
    val body: Element by element(Expression) {
        needTransformMethod()
        visitorParameterName = "body"
        transformByChildren = true
        typeKind = TypeKind.Class
    }
    val expressionBody: Element by element(Expression) {
        needTransformMethod()
        visitorParameterName = "body"
        generateIrFactoryMethod = true

        parent(body)

        +factory
        +field("expression", expression) {
            useFieldInIrFactory()
        }
    }
    val blockBody: Element by element(Expression) {
        visitorParameterName = "body"
        generateIrFactoryMethod = true

        parent(body)
        parent(statementContainer)

        +factory
    }
    val declarationReference: Element by element(Expression) {
        parent(expression)

        +symbol(symbolType)
        //diff: no accept
    }
    val memberAccessExpression: Element by element(Expression) {
        nameInVisitorMethod = "MemberAccess"
        transformerReturnType = rootElement
        val s = +param("S", symbolType)

        parent(declarationReference)

        +field("dispatchReceiver", expression, nullable = true) {
            baseDefaultValue = "null"
        }
        +field("extensionReceiver", expression, nullable = true) {
            baseDefaultValue = "null"
        }
        +symbol(s)
        +field("origin", statementOriginType, nullable = true)
        +listField("valueArguments", expression.copy(nullable = true), mutability = Array) {
            visibility = Visibility.PROTECTED
        }
        +listField("typeArguments", irTypeType.copy(nullable = true), mutability = Array) {
            visibility = Visibility.PROTECTED
        }

        generationCallback = {
            addImport(ArbitraryImportable(Packages.exprs, "checkArgumentSlotAccess"))
            val indexParam = FunctionParameter("index", StandardTypes.int)
            val valueArgumentParam = FunctionParameter("valueArgument", expression.copy(nullable = true))
            val typeArgumentParam = FunctionParameter("type", irTypeType.copy(nullable = true))

            fun printSizeProperty(listName: String) {
                println()
                println("val ", listName, "Count: Int")
                withIndent {
                    println("get() = ", listName, ".size")
                }
            }

            printSizeProperty("valueArguments")
            printSizeProperty("typeArguments")

            fun printFunction(
                name: String,
                additionalParameter: FunctionParameter?,
                returnType: TypeRefWithNullability,
                vararg statements: String,
            ) {
                println()
                printFunctionWithBlockBody(name, listOf(indexParam) + listOfNotNull(additionalParameter), returnType) {
                    statements.forEach { println(it) }
                }
            }

            printFunction(
                "getValueArgument",
                null,
                expression.copy(nullable = true),
                "checkArgumentSlotAccess(\"value\", index, valueArguments.size)",
                "return valueArguments[index]",
            )
            printFunction(
                "getTypeArgument",
                null,
                irTypeType.copy(nullable = true),
                "checkArgumentSlotAccess(\"type\", index, typeArguments.size)",
                "return typeArguments[index]",
            )
            printFunction(
                "putValueArgument",
                valueArgumentParam,
                StandardTypes.unit,
                "checkArgumentSlotAccess(\"value\", index, valueArguments.size)",
                "valueArguments[index] = valueArgument",
            )
            printFunction(
                "putTypeArgument",
                typeArgumentParam,
                StandardTypes.unit,
                "checkArgumentSlotAccess(\"type\", index, typeArguments.size)",
                "typeArguments[index] = type",
            )
        }
    }
    val functionAccessExpression: Element by element(Expression) {
        nameInVisitorMethod = "FunctionAccess"
        transformerReturnType = rootElement

        parent(memberAccessExpression.withArgs("S" to functionSymbolType))

        +field("contextReceiversCount", int)
    }
    val constructorCall: Element by element(Expression) {
        transformerReturnType = rootElement

        parent(functionAccessExpression)

        +symbol(constructorSymbolType, mutable = true)
        +field("source", type<SourceElement>()) {
            useFieldInIrFactory(defaultValue = "SourceElement.NO_SOURCE")
        }
        +field("constructorTypeArgumentsCount", int)
    }
    val getSingletonValue: Element by element(Expression) {
        nameInVisitorMethod = "SingletonReference"

        parent(declarationReference)
    }
    val getObjectValue: Element by element(Expression) {

        parent(getSingletonValue)

        +symbol(classSymbolType, mutable = true)
    }
    val getEnumValue: Element by element(Expression) {

        parent(getSingletonValue)

        +symbol(enumEntrySymbolType, mutable = true)
    }

    val rawFunctionReference: Element by element(Expression) {
        parent(declarationReference)

        kDoc = """
        Represents a platform-specific low-level reference to a function.
        
        On the JS platform it represents a plain reference to a JavaScript function.
        
        On the JVM platform it represents a [java.lang.invoke.MethodHandle] constant.
        """.trimIndent()

        +symbol(functionSymbolType, mutable = true)
    }
    val containerExpression: Element by element(Expression) {
        parent(expression)
        parent(statementContainer)

        +field("origin", statementOriginType, nullable = true)
        +listField("statements", statement, mutability = MutableList) {
            baseDefaultValue = "ArrayList(2)"
        }
    }
    val block: Element by element(Expression) {
        needAcceptMethod()

        parent(containerExpression)
    }
    val composite: Element by element(Expression) {
        parent(containerExpression)
    }
    val returnableBlock: Element by element(Expression) {
        parent(block)
        parent(symbolOwner)
        parent(returnTarget)

        +symbol(returnableBlockSymbolType)
    }
    val inlinedFunctionBlock: Element by element(Expression) {
        parent(block)

        visitorParameterName = "inlinedBlock"

        +field("inlineCall", functionAccessExpression, isChild = false)
        +field("inlinedElement", rootElement, isChild = false)
    }
    val syntheticBody: Element by element(Expression) {
        visitorParameterName = "body"

        parent(body)

        +field("kind", type(Packages.exprs, "IrSyntheticBodyKind"))
    }
    val breakContinue: Element by element(Expression) {
        visitorParameterName = "jump"

        parent(expression)

        +field("loop", loop, isChild = false)
        +field("label", string, nullable = true) {
            baseDefaultValue = "null"
        }
    }
    val `break` by element(Expression) {
        visitorParameterName = "jump"

        parent(breakContinue)
    }
    val `continue` by element(Expression) {
        visitorParameterName = "jump"

        parent(breakContinue)
    }
    val call: Element by element(Expression) {
        parent(functionAccessExpression)

        +symbol(simpleFunctionSymbolType, mutable = true)
        +field("superQualifierSymbol", classSymbolType, nullable = true)
    }
    val callableReference: Element by element(Expression) {
        val s = +param("S", symbolType)

        parent(memberAccessExpression.withArgs("S" to s))

        +symbol(s, mutable = true)
    }
    val functionReference: Element by element(Expression) {

        parent(callableReference.withArgs("S" to functionSymbolType))

        +field("reflectionTarget", functionSymbolType, nullable = true)
    }
    val propertyReference: Element by element(Expression) {
        parent(callableReference.withArgs("S" to propertySymbolType))

        +field("field", fieldSymbolType, nullable = true)
        +field("getter", simpleFunctionSymbolType, nullable = true)
        +field("setter", simpleFunctionSymbolType, nullable = true)
    }
    val localDelegatedPropertyReference: Element by element(Expression) {
        parent(callableReference.withArgs("S" to localDelegatedPropertySymbolType))

        +field("delegate", variableSymbolType)
        +field("getter", simpleFunctionSymbolType)
        +field("setter", simpleFunctionSymbolType, nullable = true)
    }
    val classReference: Element by element(Expression) {
        parent(declarationReference)

        +symbol(classifierSymbolType, mutable = true)
        +field("classType", irTypeType)
    }
    val const: Element by element(Expression) {
        val t = +param("T")

        parent(expression)

        +field("kind", type(Packages.exprs, "IrConstKind").withArgs(t))
        +field("value", t)
    }
    val constantValue: Element by element(Expression) {
        transformByChildren = true

        parent(expression)

        generationCallback = {
            println()
            printFunctionDeclaration(
                name = "contentEquals",
                parameters = listOf(FunctionParameter("other", constantValue)),
                returnType = StandardTypes.boolean,
                modality = Modality.ABSTRACT,
            )
            println()
            println()
            printFunctionDeclaration(
                name = "contentHashCode",
                parameters = emptyList(),
                returnType = StandardTypes.int,
                modality = Modality.ABSTRACT,
            )
            println()
        }
    }
    val constantPrimitive: Element by element(Expression) {
        parent(constantValue)

        +field("value", const.withArgs("T" to TypeRef.Star))
    }
    val constantObject: Element by element(Expression) {
        parent(constantValue)

        +field("constructor", constructorSymbolType)
        +listField("valueArguments", constantValue, mutability = MutableList)
        +listField("typeArguments", irTypeType, mutability = MutableList)
    }
    val constantArray: Element by element(Expression) {
        parent(constantValue)

        +listField("elements", constantValue, mutability = MutableList)
    }
    val delegatingConstructorCall: Element by element(Expression) {
        parent(functionAccessExpression)

        +symbol(constructorSymbolType, mutable = true)
    }
    val dynamicExpression: Element by element(Expression) {
        parent(expression)
    }
    val dynamicOperatorExpression: Element by element(Expression) {
        parent(dynamicExpression)

        +field("operator", type(Packages.exprs, "IrDynamicOperator"))
        +field("receiver", expression)
        +listField("arguments", expression, mutability = MutableList)
    }
    val dynamicMemberExpression: Element by element(Expression) {
        parent(dynamicExpression)

        +field("memberName", string)
        +field("receiver", expression)
    }
    val enumConstructorCall: Element by element(Expression) {
        parent(functionAccessExpression)

        +symbol(constructorSymbolType, mutable = true)
    }
    val errorExpression: Element by element(Expression) {
        needAcceptMethod()

        parent(expression)

        +field("description", string)
    }
    val errorCallExpression: Element by element(Expression) {
        parent(errorExpression)

        +field("explicitReceiver", expression, nullable = true)
        +listField("arguments", expression, mutability = MutableList)
    }
    val fieldAccessExpression: Element by element(Expression) {
        nameInVisitorMethod = "FieldAccess"
        ownsChildren = false

        parent(declarationReference)

        +symbol(fieldSymbolType, mutable = true)
        +field("superQualifierSymbol", classSymbolType, nullable = true)
        +field("receiver", expression, nullable = true) {
            baseDefaultValue = "null"
        }
        +field("origin", statementOriginType, nullable = true)
    }
    val getField: Element by element(Expression) {
        parent(fieldAccessExpression)
    }
    val setField: Element by element(Expression) {
        parent(fieldAccessExpression)

        +field("value", expression)
    }
    val functionExpression: Element by element(Expression) {
        transformerReturnType = rootElement

        parent(expression)

        +field("origin", statementOriginType)
        +field("function", simpleFunction)
    }
    val getClass: Element by element(Expression) {
        parent(expression)

        +field("argument", expression)
    }
    val instanceInitializerCall: Element by element(Expression) {
        parent(expression)

        +field("classSymbol", classSymbolType)
    }
    val loop: Element by element(Expression) {
        visitorParameterName = "loop"
        ownsChildren = false

        parent(expression)

        +field("origin", statementOriginType, nullable = true)
        +field("body", expression, nullable = true) {
            baseDefaultValue = "null"
        }
        +field("condition", expression)
        +field("label", string, nullable = true) {
            baseDefaultValue = "null"
        }
    }
    val whileLoop: Element by element(Expression) {
        visitorParameterName = "loop"
        childrenOrderOverride = listOf("condition", "body")

        parent(loop)
    }
    val doWhileLoop: Element by element(Expression) {
        visitorParameterName = "loop"

        parent(loop)
    }
    val `return`: Element by element(Expression) {
        parent(expression)

        +field("value", expression)
        +field("returnTargetSymbol", returnTargetSymbolType)
    }
    val stringConcatenation: Element by element(Expression) {
        parent(expression)

        kDoc = """
        Represents a string template expression.
        
        For example, the value of `template` in the following code:
        ```kotlin
        val i = 10
        val template = "i = ${'$'}i"
        ```
        will be represented by [${typeName}] with the following list of [arguments]:
        - [${const.typeName}] whose `value` is `"i = "`
        - [${getValue.typeName}] whose `symbol` will be that of the `i` variable. 
        """.trimIndent()

        +listField("arguments", expression, mutability = MutableList)
    }
    val suspensionPoint: Element by element(Expression) {
        parent(expression)

        +field("suspensionPointIdParameter", variable)
        +field("result", expression)
        +field("resumeResult", expression)
    }
    val suspendableExpression: Element by element(Expression) {
        parent(expression)

        +field("suspensionPointId", expression)
        +field("result", expression)
    }
    val `throw`: Element by element(Expression) {
        parent(expression)

        +field("value", expression)
    }
    val `try`: Element by element(Expression) {
        visitorParameterName = "aTry"

        parent(expression)

        +field("tryResult", expression)
        +listField("catches", catch, mutability = MutableList)
        +field("finallyExpression", expression, nullable = true)
    }
    val catch: Element by element(Expression) {
        visitorParameterName = "aCatch"
        needTransformMethod()
        transformByChildren = true

        +field("catchParameter", variable)
        +field("result", expression)
    }
    val typeOperatorCall: Element by element(Expression) {
        nameInVisitorMethod = "TypeOperator"

        parent(expression)

        +field("operator", type(Packages.exprs, "IrTypeOperator"))
        +field("argument", expression)
        +field("typeOperand", irTypeType)
    }
    val valueAccessExpression: Element by element(Expression) {
        nameInVisitorMethod = "ValueAccess"

        parent(declarationReference)

        +symbol(valueSymbolType, mutable = true)
        +field("origin", statementOriginType, nullable = true)
    }
    val getValue: Element by element(Expression) {
        parent(valueAccessExpression)
    }
    val setValue: Element by element(Expression) {
        parent(valueAccessExpression)

        +field("value", expression)
    }
    val varargElement: Element by element(Expression)
    val vararg: Element by element(Expression) {
        parent(expression)

        +field("varargElementType", irTypeType)
        +listField("elements", varargElement, mutability = MutableList)
    }
    val spreadElement: Element by element(Expression) {
        visitorParameterName = "spread"
        needTransformMethod()
        transformByChildren = true

        parent(varargElement)

        +field("expression", expression)
    }
    val `when`: Element by element(Expression) {
        parent(expression)

        +field("origin", statementOriginType, nullable = true)
        +listField("branches", branch, mutability = MutableList)
    }
    val branch: Element by element(Expression) {
        visitorParameterName = "branch"
        needAcceptMethod()
        needTransformMethod()
        transformByChildren = true

        +field("condition", expression)
        +field("result", expression)
    }
    val elseBranch: Element by element(Expression) {
        visitorParameterName = "branch"
        needTransformMethod()
        transformByChildren = true

        parent(branch)
    }
}
