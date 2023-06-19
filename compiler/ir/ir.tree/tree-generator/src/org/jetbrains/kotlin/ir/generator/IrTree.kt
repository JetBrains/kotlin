/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator

import com.squareup.kotlinpoet.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.ValueClassRepresentation
import org.jetbrains.kotlin.ir.generator.config.AbstractTreeBuilder
import org.jetbrains.kotlin.ir.generator.config.ElementConfig
import org.jetbrains.kotlin.ir.generator.config.ElementConfig.Category.*
import org.jetbrains.kotlin.ir.generator.config.ListFieldConfig.Mutability.Array
import org.jetbrains.kotlin.ir.generator.config.ListFieldConfig.Mutability.List
import org.jetbrains.kotlin.ir.generator.config.ListFieldConfig.Mutability.Var
import org.jetbrains.kotlin.ir.generator.config.SimpleFieldConfig
import org.jetbrains.kotlin.ir.generator.model.Element.Companion.elementName2typeName
import org.jetbrains.kotlin.ir.generator.print.IR_FACTORY_TYPE
import org.jetbrains.kotlin.ir.generator.print.toPoet
import org.jetbrains.kotlin.ir.generator.util.*
import org.jetbrains.kotlin.ir.generator.util.Import
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.types.Variance

// Note the style of the DSL to describe IR elements, which is these things in the following order:
// 1) config (see properties of ElementConfig)
// 2) parents
// 3) fields
object IrTree : AbstractTreeBuilder() {
    private fun symbol(type: TypeRef, mutable: Boolean = false): SimpleFieldConfig =
        field("symbol", type, mutable = mutable)

    private fun descriptor(typeName: String, nullable: Boolean = false): SimpleFieldConfig =
        field(
            name = "descriptor",
            type = ClassRef<TypeParameterRef>(TypeKind.Interface, Packages.descriptors, typeName),
            mutable = false,
            nullable = nullable,
        ) {
            skipInIrFactory()
        }

    private fun declarationWithLateBinding(symbol: ClassRef<*>, initializer: ElementConfig.() -> Unit) = element(Declaration) {
        initializer()

        fieldsToSkipInIrFactoryMethod.add("symbol")
        fieldsToSkipInIrFactoryMethod.add("containerSource")

        +field("isBound", boolean, mutable = false) {
            skipInIrFactory()
        }

        val oldCallback = generationCallback
        generationCallback = {
            oldCallback?.invoke(this)
            addFunction(
                FunSpec.builder("acquireSymbol")
                    .addModifiers(KModifier.ABSTRACT)
                    .addParameter("symbol", symbol.toPoet())
                    .returns(this@element.toPoet())
                    .build(),
            )
        }
    }

    private val factory: SimpleFieldConfig = field("factory", IR_FACTORY_TYPE, mutable = false) {
        skipInIrFactory()
    }

    override val rootElement: ElementConfig by element(Other, name = "element") {
        accept = true
        transform = true
        transformByChildren = true

        fun offsetField(prefix: String) = field(prefix + "Offset", int, mutable = false) {
            kdoc = """
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
    val statement: ElementConfig by element(Other)

    val declaration: ElementConfig by element(Declaration) {
        parent(statement)
        parent(symbolOwner)
        parent(mutableAnnotationContainerType)

        +descriptor("DeclarationDescriptor")
        +field("origin", type(Packages.declarations, "IrDeclarationOrigin"))
        +field("parent", declarationParent) {
            skipInIrFactory()
        }
        +factory
    }
    val declarationBase: ElementConfig by element(Declaration) {
        typeKind = TypeKind.Class
        transformByChildren = true
        transformerReturnType = statement
        visitorParent = rootElement
        visitorName = "declaration"

        parent(declaration)
    }
    val declarationParent: ElementConfig by element(Declaration)
    val declarationWithVisibility: ElementConfig by element(Declaration) {
        parent(declaration)

        +field("visibility", type(Packages.descriptors, "DescriptorVisibility"))
    }
    val declarationWithName: ElementConfig by element(Declaration) {
        parent(declaration)

        +field("name", type<Name>())
    }
    val possiblyExternalDeclaration: ElementConfig by element(Declaration) {
        parent(declarationWithName)

        +field("isExternal", boolean) {
            useFieldInIrFactory(false)
        }
    }
    val symbolOwner: ElementConfig by element(Declaration) {
        +symbol(symbolType)
    }
    val metadataSourceOwner: ElementConfig by element(Declaration) {
        val metadataField = +field("metadata", type(Packages.declarations, "MetadataSource"), nullable = true) {
            skipInIrFactory()
            kdoc = """
            The arbitrary metadata associated with this IR node.
            
            @see ${elementName2typeName(this@element.name)}
            """.trimIndent()
        }
        kDoc = """
        An [${elementName2typeName(rootElement.name)}] capable of holding something which backends can use to write
        as the metadata for the declaration.
        
        Technically, it can even be ± an array of bytes, but right now it's usually the frontend representation of the declaration,
        so a descriptor in case of K1, and [org.jetbrains.kotlin.fir.FirElement] in case of K2,
        and the backend invokes a metadata serializer on it to obtain metadata and write it, for example, to `@kotlin.Metadata`
        on JVM.
        
        In Kotlin/Native, [${metadataField.name}] is used to store some LLVM-related stuff in an IR declaration,
        but this is only for performance purposes (before it was done using simple maps).
        """.trimIndent()
    }
    val overridableMember: ElementConfig by element(Declaration) {
        parent(declaration)
        parent(declarationWithVisibility)
        parent(declarationWithName)
        parent(symbolOwner)

        +field("modality", type<Modality>())
    }
    val overridableDeclaration: ElementConfig by element(Declaration) {
        val s = +param("S", symbolType)

        parent(overridableMember)

        +field("symbol", s, mutable = false)
        +isFakeOverrideField()
        +listField("overriddenSymbols", s, mutability = Var) {
            skipInIrFactory()
        }
    }
    val memberWithContainerSource: ElementConfig by element(Declaration) {
        parent(declarationWithName)

        +field("containerSource", type<DeserializedContainerSource>(), nullable = true, mutable = false) {
            useFieldInIrFactory(defaultValue = code("null"))
        }
    }
    val valueDeclaration: ElementConfig by element(Declaration) {
        parent(declarationWithName)
        parent(symbolOwner)

        +descriptor("ValueDescriptor")
        +symbol(valueSymbolType)
        +field("type", irTypeType)
        +field("isAssignable", boolean, mutable = false)
    }
    val valueParameter: ElementConfig by element(Declaration) {
        transform = true
        visitorParent = declarationBase

        parent(declarationBase)
        parent(valueDeclaration)

        +descriptor("ParameterDescriptor")
        +symbol(valueParameterSymbolType)
        +field("index", int)
        +field("varargElementType", irTypeType, nullable = true)
        +field("isCrossinline", boolean)
        +field("isNoinline", boolean)
        +field("isHidden", boolean) {
            additionalImports.add(Import("org.jetbrains.kotlin.ir.util", "IdSignature"))
            kdoc = """
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

            If a compiler plugin adds parameters to an [${elementName2typeName(function.name)}],
            the representations of the function in the frontend and in the backend may diverge, potentially causing signature mismatch and
            linkage errors (see [KT-40980](https://youtrack.jetbrains.com/issue/KT-40980)).
            We wouldn't want IR plugins to affect the frontend representation, since in an IDE you'd want to be able to see those
            declarations in their original form (without the `${'$'}extra` parameter).

            To fix this problem, [$name] was introduced.
            
            TODO: consider dropping [$name] if it isn't used by any known plugin.
            """.trimIndent()
        }
        +field("defaultValue", expressionBody, nullable = true, isChild = true)
    }
    val `class`: ElementConfig by element(Declaration) {
        visitorParent = declarationBase

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
            useFieldInIrFactory(defaultValue = false)
        }
        +field("isInner", boolean) {
            useFieldInIrFactory(defaultValue = false)
        }
        +field("isData", boolean) {
            useFieldInIrFactory(defaultValue = false)
        }
        +field("isValue", boolean) {
            useFieldInIrFactory(defaultValue = false)
        }
        +field("isExpect", boolean) {
            useFieldInIrFactory(defaultValue = false)
        }
        +field("isFun", boolean) {
            useFieldInIrFactory(defaultValue = false)
        }
        +field("source", type<SourceElement>(), mutable = false) {
            useFieldInIrFactory(defaultValue = code("%T.NO_SOURCE", SourceElement::class))
        }
        +listField("superTypes", irTypeType, mutability = Var) {
            skipInIrFactory()
        }
        +field("thisReceiver", valueParameter, nullable = true, isChild = true)
        +field(
            "valueClassRepresentation",
            type<ValueClassRepresentation<*>>().withArgs(type(Packages.types, "IrSimpleType")),
            nullable = true,
        ) {
            skipInIrFactory()
        }
        +listField("sealedSubclasses", classSymbolType, mutability = Var) {
            skipInIrFactory()
            kdoc = """
            If this is a sealed class or interface, this list contains symbols of all its immediate subclasses.
            Otherwise, this is an empty list.
            
            NOTE: If this [${elementName2typeName(this@element.name)}] was deserialized from a klib, this list will always be empty!
            See [KT-54028](https://youtrack.jetbrains.com/issue/KT-54028).
            """.trimIndent()
        }
    }
    val attributeContainer: ElementConfig by element(Declaration) {
        kDoc = """
            Represents an IR element that can be copied, but must remember its original element. It is
            useful, for example, to keep track of generated names for anonymous declarations.
            @property attributeOwnerId original element before copying. Always satisfies the following
              invariant: `this.attributeOwnerId == this.attributeOwnerId.attributeOwnerId`.
            @property originalBeforeInline original element before inlining. Useful only with IR
              inliner. `null` if the element wasn't inlined. Unlike [attributeOwnerId], doesn't have the
              idempotence invariant and can contain a chain of declarations.
        """.trimIndent()

        +field("attributeOwnerId", attributeContainer) {
            skipInIrFactory()
        }
        // null <=> this element wasn't inlined
        +field("originalBeforeInline", attributeContainer, nullable = true) {
            skipInIrFactory()
        }
    }
    val anonymousInitializer: ElementConfig by element(Declaration) {
        visitorParent = declarationBase

        parent(declarationBase)

        +descriptor("ClassDescriptor") // TODO special descriptor for anonymous initializer blocks
        +symbol(anonymousInitializerSymbolType)
        +field("isStatic", boolean) {
            useFieldInIrFactory(defaultValue = false)
        }
        +field("body", blockBody, isChild = true)
    }
    val declarationContainer: ElementConfig by element(Declaration) {
        ownsChildren = false

        parent(declarationParent)

        +listField("declarations", declaration, mutability = List, isChild = true)
    }
    val typeParametersContainer: ElementConfig by element(Declaration) {
        ownsChildren = false

        parent(declaration)
        parent(declarationParent)

        +listField("typeParameters", typeParameter, mutability = Var, isChild = true)
    }
    val typeParameter: ElementConfig by element(Declaration) {
        visitorParent = declarationBase
        transform = true

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
    val returnTarget: ElementConfig by element(Declaration) {
        parent(symbolOwner)

        +descriptor("FunctionDescriptor")
        +symbol(returnTargetSymbolType)
    }
    val function: ElementConfig by element(Declaration) {
        visitorParent = declarationBase

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
        +field("dispatchReceiverParameter", valueParameter, nullable = true, isChild = true)
        +field("extensionReceiverParameter", valueParameter, nullable = true, isChild = true)
        +listField("valueParameters", valueParameter, mutability = Var, isChild = true)
        // The first `contextReceiverParametersCount` value parameters are context receivers.
        +field("contextReceiverParametersCount", int) {
            skipInIrFactory()
        }
        +field("body", body, nullable = true, isChild = true)
    }
    val constructor: ElementConfig by element(Declaration) {
        visitorParent = function

        parent(function)

        +descriptor("ClassConstructorDescriptor")
        +symbol(constructorSymbolType)
        +field("isPrimary", boolean)
    }
    val enumEntry: ElementConfig by element(Declaration) {
        visitorParent = declarationBase

        parent(declarationBase)
        parent(declarationWithName)

        +descriptor("ClassDescriptor")
        +symbol(enumEntrySymbolType)
        +field("initializerExpression", expressionBody, nullable = true, isChild = true)
        +field("correspondingClass", `class`, nullable = true, isChild = true)
    }
    val errorDeclaration: ElementConfig by element(Declaration) {
        visitorParent = declarationBase

        parent(declarationBase)

        additionalIrFactoryMethodParameters.add(
            descriptor("DeclarationDescriptor", nullable = true).apply {
                useFieldInIrFactory(defaultValue = code("null"))
            }
        )

        fieldsToSkipInIrFactoryMethod.add("origin")

        +field("symbol", symbolType, mutable = false) {
            baseGetter = code("error(\"Should never be called\")")
            skipInIrFactory()
        }
    }
    val functionWithLateBinding: ElementConfig by declarationWithLateBinding(simpleFunctionSymbolType) {
        parent(simpleFunction)
    }
    val propertyWithLateBinding: ElementConfig by declarationWithLateBinding(propertySymbolType) {
        parent(property)
    }
    val field: ElementConfig by element(Declaration) {
        visitorParent = declarationBase

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
        +field("initializer", expressionBody, nullable = true, isChild = true)
        +field("correspondingPropertySymbol", propertySymbolType, nullable = true) {
            skipInIrFactory()
        }
    }
    val localDelegatedProperty: ElementConfig by element(Declaration) {
        visitorParent = declarationBase

        parent(declarationBase)
        parent(declarationWithName)
        parent(symbolOwner)
        parent(metadataSourceOwner)

        +descriptor("VariableDescriptorWithAccessors")
        +symbol(localDelegatedPropertySymbolType)
        +field("type", irTypeType)
        +field("isVar", boolean)
        +field("delegate", variable, isChild = true)
        +field("getter", simpleFunction, isChild = true)
        +field("setter", simpleFunction, nullable = true, isChild = true)
    }
    val moduleFragment: ElementConfig by element(Declaration) {
        visitorParent = rootElement
        transform = true
        transformByChildren = true
        generateIrFactoryMethod = false

        +descriptor("ModuleDescriptor")
        +field("name", type<Name>(), mutable = false)
        +field("irBuiltins", type(Packages.tree, "IrBuiltIns"), mutable = false)
        +listField("files", file, mutability = List, isChild = true)
        val undefinedOffset = MemberName(Packages.tree, "UNDEFINED_OFFSET")
        +field("startOffset", int, mutable = false) {
            baseGetter = code("%M", undefinedOffset)
        }
        +field("endOffset", int, mutable = false) {
            baseGetter = code("%M", undefinedOffset)
        }
    }
    val property: ElementConfig by element(Declaration) {
        visitorParent = declarationBase
        isForcedLeaf = true

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
            useFieldInIrFactory(defaultValue = false)
        }
        +isFakeOverrideField()
        +field("backingField", field, nullable = true, isChild = true)
        +field("getter", simpleFunction, nullable = true, isChild = true)
        +field("setter", simpleFunction, nullable = true, isChild = true)
    }

    private fun isFakeOverrideField() = field("isFakeOverride", boolean) {
        useFieldInIrFactory(
            defaultValue = code(
                "origin == %T.FAKE_OVERRIDE",
                type(Packages.declarations, "IrDeclarationOrigin").toPoet(),
            ),
        )
    }

    //TODO: make IrScript as IrPackageFragment, because script is used as a file, not as a class
    //NOTE: declarations and statements stored separately
    val script: ElementConfig by element(Declaration) {
        visitorParent = declarationBase
        generateIrFactoryMethod = false

        parent(declarationBase)
        parent(declarationWithName)
        parent(declarationParent)
        parent(statementContainer)
        parent(metadataSourceOwner)

        +symbol(scriptSymbolType)
        // NOTE: is the result of the FE conversion, because there script interpreted as a class and has receiver
        // TODO: consider removing from here and handle appropriately in the lowering
        +field("thisReceiver", valueParameter, isChild = true, nullable = true) // K1
        +field("baseClass", irTypeType, nullable = true) // K1
        +listField("explicitCallParameters", variable, mutability = Var, isChild = true)
        +listField("implicitReceiversParameters", valueParameter, mutability = Var, isChild = true)
        +listField("providedProperties", propertySymbolType, mutability = Var)
        +listField("providedPropertiesParameters", valueParameter, mutability = Var, isChild = true)
        +field("resultProperty", propertySymbolType, nullable = true)
        +field("earlierScriptsParameter", valueParameter, nullable = true, isChild = true)
        +listField("importedScripts", scriptSymbolType, mutability = Var, nullable = true)
        +listField("earlierScripts", scriptSymbolType, mutability = Var, nullable = true)
        +field("targetClass", classSymbolType, nullable = true)
        +field("constructor", constructor, nullable = true) // K1
    }
    val simpleFunction: ElementConfig by element(Declaration) {
        visitorParent = function
        isForcedLeaf = true

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
    val typeAlias: ElementConfig by element(Declaration) {
        visitorParent = declarationBase

        parent(declarationBase)
        parent(declarationWithName)
        parent(declarationWithVisibility)
        parent(typeParametersContainer)

        +descriptor("TypeAliasDescriptor")
        +symbol(typeAliasSymbolType)
        +field("isActual", boolean)
        +field("expandedType", irTypeType)
    }
    val variable: ElementConfig by element(Declaration) {
        visitorParent = declarationBase

        generateIrFactoryMethod = false

        parent(declarationBase)
        parent(valueDeclaration)

        +descriptor("VariableDescriptor")
        +symbol(variableSymbolType)
        +field("isVar", boolean)
        +field("isConst", boolean)
        +field("isLateinit", boolean)
        +field("initializer", expression, nullable = true, isChild = true)
    }
    val packageFragment: ElementConfig by element(Declaration) {
        visitorParent = rootElement
        ownsChildren = false

        parent(declarationContainer)
        parent(symbolOwner)

        +symbol(packageFragmentSymbolType)
        +field("packageFragmentDescriptor", type(Packages.descriptors, "PackageFragmentDescriptor"), mutable = false)
        +field("packageFqName", type<FqName>())
        +field("fqName", type<FqName>()) {
            baseGetter = code("packageFqName")
            generationCallback = {
                val deprecatedAnnotation = AnnotationSpec.builder(Deprecated::class)
                    .addMember(code("message = \"Please use `packageFqName` instead\""))
                    .addMember(code("replaceWith = ReplaceWith(\"packageFqName\")"))
                    .addMember(code("level = DeprecationLevel.ERROR"))
                    .build()
                addAnnotation(deprecatedAnnotation)
                setter(FunSpec.setterBuilder().addParameter("value", FqName::class).addCode(code("packageFqName = value")).build())
            }
        }
    }
    val externalPackageFragment: ElementConfig by element(Declaration) {
        visitorParent = packageFragment
        transformByChildren = true
        generateIrFactoryMethod = false

        parent(packageFragment)

        +symbol(externalPackageFragmentSymbolType)
        +field("containerSource", type<DeserializedContainerSource>(), nullable = true, mutable = false)
    }
    val file: ElementConfig by element(Declaration) {
        transform = true
        transformByChildren = true
        visitorParent = packageFragment
        generateIrFactoryMethod = false

        parent(packageFragment)
        parent(mutableAnnotationContainerType)
        parent(metadataSourceOwner)

        +symbol(fileSymbolType)
        +field("module", moduleFragment)
        +field("fileEntry", type(Packages.tree, "IrFileEntry"))
    }

    val expression: ElementConfig by element(Expression) {
        visitorParent = rootElement
        transform = true
        transformByChildren = true

        parent(statement)
        parent(varargElement)
        parent(attributeContainer)

        +field("attributeOwnerId", attributeContainer) {
            baseDefaultValue = code("this")
            skipInIrFactory()
        }
        +field("originalBeforeInline", attributeContainer, nullable = true) {
            baseDefaultValue = code("null")
            skipInIrFactory()
        }
        +field("type", irTypeType)
    }
    val statementContainer: ElementConfig by element(Expression) {
        ownsChildren = false

        +listField("statements", statement, mutability = List, isChild = true)
    }
    val body: ElementConfig by element(Expression) {
        transform = true
        visitorParent = rootElement
        visitorParam = "body"
        transformByChildren = true
        typeKind = TypeKind.Class
    }
    val expressionBody: ElementConfig by element(Expression) {
        transform = true
        visitorParent = body
        visitorParam = "body"
        generateIrFactoryMethod = true

        parent(body)

        +factory
        +field("expression", expression, isChild = true) {
            useFieldInIrFactory()
        }
    }
    val blockBody: ElementConfig by element(Expression) {
        visitorParent = body
        visitorParam = "body"
        generateIrFactoryMethod = true

        parent(body)
        parent(statementContainer)

        +factory
    }
    val declarationReference: ElementConfig by element(Expression) {
        visitorParent = expression

        parent(expression)

        +symbol(symbolType)
        //diff: no accept
    }
    val memberAccessExpression: ElementConfig by element(Expression) {
        visitorParent = declarationReference
        visitorName = "memberAccess"
        transformerReturnType = rootElement
        val s = +param("S", symbolType)

        parent(declarationReference)

        +field("dispatchReceiver", expression, nullable = true, isChild = true) {
            baseDefaultValue = code("null")
        }
        +field("extensionReceiver", expression, nullable = true, isChild = true) {
            baseDefaultValue = code("null")
        }
        +symbol(s)
        +field("origin", statementOriginType, nullable = true)
        +listField("valueArguments", expression.copy(nullable = true), mutability = Array, isChild = true) {
            generationCallback = {
                addModifiers(KModifier.PROTECTED)
            }
        }
        +listField("typeArguments", irTypeType.copy(nullable = true), mutability = Array) {
            generationCallback = {
                addModifiers(KModifier.PROTECTED)
            }
        }

        val checkArgumentSlotAccess = MemberName("org.jetbrains.kotlin.ir.expressions", "checkArgumentSlotAccess", true)
        generationCallback = {
            addFunction(
                FunSpec.builder("getValueArgument")
                    .addParameter("index", int.toPoet())
                    .returns(expression.toPoet().copy(nullable = true))
                    .addCode("%M(\"value\", index, valueArguments.size)\n", checkArgumentSlotAccess)
                    .addCode("return valueArguments[index]")
                    .build()
            )
            addFunction(
                FunSpec.builder("getTypeArgument")
                    .addParameter("index", int.toPoet())
                    .returns(irTypeType.toPoet().copy(nullable = true))
                    .addCode("%M(\"type\", index, typeArguments.size)\n", checkArgumentSlotAccess)
                    .addCode("return typeArguments[index]")
                    .build()
            )
            addFunction(
                FunSpec.builder("putValueArgument")
                    .addParameter("index", int.toPoet())
                    .addParameter("valueArgument", expression.toPoet().copy(nullable = true))
                    .addCode("%M(\"value\", index, valueArguments.size)\n", checkArgumentSlotAccess)
                    .addCode("valueArguments[index] = valueArgument")
                    .build()
            )
            addFunction(
                FunSpec.builder("putTypeArgument")
                    .addParameter("index", int.toPoet())
                    .addParameter("type", irTypeType.toPoet().copy(nullable = true))
                    .addCode("%M(\"type\", index, typeArguments.size)\n", checkArgumentSlotAccess)
                    .addCode("typeArguments[index] = type")
                    .build()
            )
            addProperty(
                PropertySpec.builder("valueArgumentsCount", int.toPoet())
                    .getter(FunSpec.getterBuilder().addCode("return valueArguments.size").build())
                    .build()
            )
            addProperty(
                PropertySpec.builder("typeArgumentsCount", int.toPoet())
                    .getter(FunSpec.getterBuilder().addCode("return typeArguments.size").build())
                    .build()
            )
        }
    }
    val functionAccessExpression: ElementConfig by element(Expression) {
        visitorParent = memberAccessExpression
        visitorName = "functionAccess"
        transformerReturnType = rootElement

        parent(memberAccessExpression.withArgs("S" to functionSymbolType))

        +field("contextReceiversCount", int)
    }
    val constructorCall: ElementConfig by element(Expression) {
        visitorParent = functionAccessExpression
        transformerReturnType = rootElement

        parent(functionAccessExpression)

        +symbol(constructorSymbolType, mutable = true)
        +field("source", type<SourceElement>()) {
            useFieldInIrFactory(defaultValue = code("%T.NO_SOURCE", SourceElement::class))
        }
        +field("constructorTypeArgumentsCount", int)
    }
    val getSingletonValue: ElementConfig by element(Expression) {
        visitorParent = declarationReference
        visitorName = "SingletonReference"

        parent(declarationReference)
    }
    val getObjectValue: ElementConfig by element(Expression) {
        visitorParent = getSingletonValue

        parent(getSingletonValue)

        +symbol(classSymbolType, mutable = true)
    }
    val getEnumValue: ElementConfig by element(Expression) {
        visitorParent = getSingletonValue

        parent(getSingletonValue)

        +symbol(enumEntrySymbolType, mutable = true)
    }

    /**
     * Platform-specific low-level reference to function.
     *
     * On JS platform it represents a plain reference to JavaScript function.
     * On JVM platform it represents a MethodHandle constant.
     */
    val rawFunctionReference: ElementConfig by element(Expression) {
        visitorParent = declarationReference

        parent(declarationReference)

        +symbol(functionSymbolType, mutable = true)
    }
    val containerExpression: ElementConfig by element(Expression) {
        visitorParent = expression

        parent(expression)
        parent(statementContainer)

        +field("origin", statementOriginType, nullable = true)
        +listField("statements", statement, mutability = List, isChild = true) {
            generationCallback = {
                addModifiers(KModifier.OVERRIDE)
            }
            baseDefaultValue = code("ArrayList(2)")
        }
    }
    val block: ElementConfig by element(Expression) {
        visitorParent = containerExpression
        accept = true

        parent(containerExpression)
    }
    val composite: ElementConfig by element(Expression) {
        visitorParent = containerExpression

        parent(containerExpression)
    }
    val returnableBlock: ElementConfig by element(Expression) {
        parent(block)
        parent(symbolOwner)
        parent(returnTarget)

        +symbol(returnableBlockSymbolType)
    }
    val inlinedFunctionBlock: ElementConfig by element(Expression) {
        parent(block)

        +field("inlineCall", functionAccessExpression)
        +field("inlinedElement", rootElement)
    }
    val syntheticBody: ElementConfig by element(Expression) {
        visitorParent = body
        visitorParam = "body"

        parent(body)

        +field("kind", type(Packages.exprs, "IrSyntheticBodyKind"))
    }
    val breakContinue: ElementConfig by element(Expression) {
        visitorParent = expression
        visitorParam = "jump"

        parent(expression)

        +field("loop", loop)
        +field("label", string, nullable = true) {
            baseDefaultValue = code("null")
        }
    }
    val `break` by element(Expression) {
        visitorParent = breakContinue
        visitorParam = "jump"

        parent(breakContinue)
    }
    val `continue` by element(Expression) {
        visitorParent = breakContinue
        visitorParam = "jump"

        parent(breakContinue)
    }
    val call: ElementConfig by element(Expression) {
        visitorParent = functionAccessExpression

        parent(functionAccessExpression)

        +symbol(simpleFunctionSymbolType, mutable = true)
        +field("superQualifierSymbol", classSymbolType, nullable = true)
    }
    val callableReference: ElementConfig by element(Expression) {
        visitorParent = memberAccessExpression
        val s = +param("S", symbolType)

        parent(memberAccessExpression.withArgs("S" to s))

        +symbol(s, mutable = true)
    }
    val functionReference: ElementConfig by element(Expression) {
        visitorParent = callableReference

        parent(callableReference.withArgs("S" to functionSymbolType))

        +field("reflectionTarget", functionSymbolType, nullable = true)
    }
    val propertyReference: ElementConfig by element(Expression) {
        visitorParent = callableReference

        parent(callableReference.withArgs("S" to propertySymbolType))

        +field("field", fieldSymbolType, nullable = true)
        +field("getter", simpleFunctionSymbolType, nullable = true)
        +field("setter", simpleFunctionSymbolType, nullable = true)
    }
    val localDelegatedPropertyReference: ElementConfig by element(Expression) {
        visitorParent = callableReference

        parent(callableReference.withArgs("S" to localDelegatedPropertySymbolType))

        +field("delegate", variableSymbolType)
        +field("getter", simpleFunctionSymbolType)
        +field("setter", simpleFunctionSymbolType, nullable = true)
    }
    val classReference: ElementConfig by element(Expression) {
        visitorParent = declarationReference

        parent(declarationReference)

        +symbol(classifierSymbolType, mutable = true)
        +field("classType", irTypeType)
    }
    val const: ElementConfig by element(Expression) {
        visitorParent = expression
        val t = +param("T")

        parent(expression)

        +field("kind", type(Packages.exprs, "IrConstKind").withArgs(t))
        +field("value", t)
    }
    val constantValue: ElementConfig by element(Expression) {
        visitorParent = expression
        transformByChildren = true

        parent(expression)

        generationCallback = {
            addFunction(
                FunSpec.builder("contentEquals")
                    .addModifiers(KModifier.ABSTRACT)
                    .addParameter("other", constantValue.toPoet())
                    .returns(boolean.toPoet())
                    .build()
            )
            addFunction(
                FunSpec.builder("contentHashCode")
                    .addModifiers(KModifier.ABSTRACT)
                    .returns(int.toPoet())
                    .build()
            )
        }
    }
    val constantPrimitive: ElementConfig by element(Expression) {
        visitorParent = constantValue

        parent(constantValue)

        +field("value", const.withArgs("T" to TypeRef.Star), isChild = true)
    }
    val constantObject: ElementConfig by element(Expression) {
        visitorParent = constantValue

        parent(constantValue)

        +field("constructor", constructorSymbolType)
        +listField("valueArguments", constantValue, mutability = List, isChild = true)
        +listField("typeArguments", irTypeType, mutability = List)
    }
    val constantArray: ElementConfig by element(Expression) {
        visitorParent = constantValue

        parent(constantValue)

        +listField("elements", constantValue, mutability = List, isChild = true)
    }
    val delegatingConstructorCall: ElementConfig by element(Expression) {
        visitorParent = functionAccessExpression

        parent(functionAccessExpression)

        +symbol(constructorSymbolType, mutable = true)
    }
    val dynamicExpression: ElementConfig by element(Expression) {
        visitorParent = expression

        parent(expression)
    }
    val dynamicOperatorExpression: ElementConfig by element(Expression) {
        visitorParent = dynamicExpression

        parent(dynamicExpression)

        +field("operator", type(Packages.exprs, "IrDynamicOperator"))
        +field("receiver", expression, isChild = true)
        +listField("arguments", expression, mutability = List, isChild = true)
    }
    val dynamicMemberExpression: ElementConfig by element(Expression) {
        visitorParent = dynamicExpression

        parent(dynamicExpression)

        +field("memberName", string)
        +field("receiver", expression, isChild = true)
    }
    val enumConstructorCall: ElementConfig by element(Expression) {
        visitorParent = functionAccessExpression

        parent(functionAccessExpression)

        +symbol(constructorSymbolType, mutable = true)
    }
    val errorExpression: ElementConfig by element(Expression) {
        visitorParent = expression
        accept = true

        parent(expression)

        +field("description", string)
    }
    val errorCallExpression: ElementConfig by element(Expression) {
        visitorParent = errorExpression

        parent(errorExpression)

        +field("explicitReceiver", expression, nullable = true, isChild = true)
        +listField("arguments", expression, mutability = List, isChild = true)
    }
    val fieldAccessExpression: ElementConfig by element(Expression) {
        visitorParent = declarationReference
        visitorName = "fieldAccess"
        ownsChildren = false

        parent(declarationReference)

        +symbol(fieldSymbolType, mutable = true)
        +field("superQualifierSymbol", classSymbolType, nullable = true)
        +field("receiver", expression, nullable = true, isChild = true) {
            baseDefaultValue = code("null")
        }
        +field("origin", statementOriginType, nullable = true)
    }
    val getField: ElementConfig by element(Expression) {
        visitorParent = fieldAccessExpression

        parent(fieldAccessExpression)
    }
    val setField: ElementConfig by element(Expression) {
        visitorParent = fieldAccessExpression

        parent(fieldAccessExpression)

        +field("value", expression, isChild = true)
    }
    val functionExpression: ElementConfig by element(Expression) {
        visitorParent = expression
        transformerReturnType = rootElement

        parent(expression)

        +field("origin", statementOriginType)
        +field("function", simpleFunction, isChild = true)
    }
    val getClass: ElementConfig by element(Expression) {
        visitorParent = expression

        parent(expression)

        +field("argument", expression, isChild = true)
    }
    val instanceInitializerCall: ElementConfig by element(Expression) {
        visitorParent = expression

        parent(expression)

        +field("classSymbol", classSymbolType)
    }
    val loop: ElementConfig by element(Expression) {
        visitorParent = expression
        visitorParam = "loop"
        ownsChildren = false

        parent(expression)

        +field("origin", statementOriginType, nullable = true)
        +field("body", expression, nullable = true, isChild = true) {
            baseDefaultValue = code("null")
        }
        +field("condition", expression, isChild = true)
        +field("label", string, nullable = true) {
            baseDefaultValue = code("null")
        }
    }
    val whileLoop: ElementConfig by element(Expression) {
        visitorParent = loop
        visitorParam = "loop"
        childrenOrderOverride = listOf("condition", "body")

        parent(loop)
    }
    val doWhileLoop: ElementConfig by element(Expression) {
        visitorParent = loop
        visitorParam = "loop"

        parent(loop)
    }
    val `return`: ElementConfig by element(Expression) {
        visitorParent = expression

        parent(expression)

        +field("value", expression, isChild = true)
        +field("returnTargetSymbol", returnTargetSymbolType)
    }
    val stringConcatenation: ElementConfig by element(Expression) {
        visitorParent = expression

        parent(expression)

        +listField("arguments", expression, mutability = List, isChild = true)
    }
    val suspensionPoint: ElementConfig by element(Expression) {
        visitorParent = expression

        parent(expression)

        +field("suspensionPointIdParameter", variable, isChild = true)
        +field("result", expression, isChild = true)
        +field("resumeResult", expression, isChild = true)
    }
    val suspendableExpression: ElementConfig by element(Expression) {
        visitorParent = expression

        parent(expression)

        +field("suspensionPointId", expression, isChild = true)
        +field("result", expression, isChild = true)
    }
    val `throw`: ElementConfig by element(Expression) {
        visitorParent = expression

        parent(expression)

        +field("value", expression, isChild = true)
    }
    val `try`: ElementConfig by element(Expression) {
        visitorParent = expression
        visitorParam = "aTry"

        parent(expression)

        +field("tryResult", expression, isChild = true)
        +listField("catches", catch, mutability = List, isChild = true)
        +field("finallyExpression", expression, nullable = true, isChild = true)
    }
    val catch: ElementConfig by element(Expression) {
        visitorParent = rootElement
        visitorParam = "aCatch"
        transform = true
        transformByChildren = true

        +field("catchParameter", variable, isChild = true)
        +field("result", expression, isChild = true)
    }
    val typeOperatorCall: ElementConfig by element(Expression) {
        visitorParent = expression
        visitorName = "typeOperator"

        parent(expression)

        +field("operator", type(Packages.exprs, "IrTypeOperator"))
        +field("argument", expression, isChild = true)
        +field("typeOperand", irTypeType)
    }
    val valueAccessExpression: ElementConfig by element(Expression) {
        visitorParent = declarationReference
        visitorName = "valueAccess"

        parent(declarationReference)

        +symbol(valueSymbolType, mutable = true)
        +field("origin", statementOriginType, nullable = true)
    }
    val getValue: ElementConfig by element(Expression) {
        visitorParent = valueAccessExpression

        parent(valueAccessExpression)
    }
    val setValue: ElementConfig by element(Expression) {
        visitorParent = valueAccessExpression

        parent(valueAccessExpression)

        +field("value", expression, isChild = true)
    }
    val varargElement: ElementConfig by element(Expression)
    val vararg: ElementConfig by element(Expression) {
        visitorParent = expression

        parent(expression)

        +field("varargElementType", irTypeType)
        +listField("elements", varargElement, mutability = List, isChild = true)
    }
    val spreadElement: ElementConfig by element(Expression) {
        visitorParent = rootElement
        visitorParam = "spread"
        transform = true
        transformByChildren = true

        parent(varargElement)

        +field("expression", expression, isChild = true)
    }
    val `when`: ElementConfig by element(Expression) {
        visitorParent = expression

        parent(expression)

        +field("origin", statementOriginType, nullable = true)
        +listField("branches", branch, mutability = List, isChild = true)
    }
    val branch: ElementConfig by element(Expression) {
        visitorParent = rootElement
        visitorParam = "branch"
        accept = true
        transform = true
        transformByChildren = true

        +field("condition", expression, isChild = true)
        +field("result", expression, isChild = true)
    }
    val elseBranch: ElementConfig by element(Expression) {
        visitorParent = branch
        visitorParam = "branch"
        transform = true
        transformByChildren = true

        parent(branch)
    }
}
