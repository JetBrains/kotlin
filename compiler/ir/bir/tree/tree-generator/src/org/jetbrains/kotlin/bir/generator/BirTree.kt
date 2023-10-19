/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.generator

import com.squareup.kotlinpoet.KModifier
import org.jetbrains.kotlin.bir.generator.config.AbstractTreeBuilder
import org.jetbrains.kotlin.bir.generator.config.ElementConfig
import org.jetbrains.kotlin.bir.generator.config.ElementConfig.Category.*
import org.jetbrains.kotlin.bir.generator.config.ListFieldConfig.Mutability.*
import org.jetbrains.kotlin.bir.generator.config.ListFieldConfig.Mutability.Array
import org.jetbrains.kotlin.bir.generator.config.ListFieldConfig.Mutability.List
import org.jetbrains.kotlin.bir.generator.config.SimpleFieldConfig
import org.jetbrains.kotlin.bir.generator.model.Element.Companion.elementName2typeName
import org.jetbrains.kotlin.bir.generator.print.toPoet
import org.jetbrains.kotlin.bir.generator.util.Import
import org.jetbrains.kotlin.bir.generator.util.code
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.ValueClassRepresentation
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.types.Variance

// Note the style of the DSL to describe IR elements, which is these things in the following order:
// 1) config (see properties of ElementConfig)
// 2) parents
// 3) fields
object BirTree : AbstractTreeBuilder() {
    private fun symbol(type: TypeRef, mutable: Boolean = false): SimpleFieldConfig =
        field("symbol", type, mutable = mutable)

    private fun descriptor(typeName: String, nullable: Boolean = true, initializer: SimpleFieldConfig.() -> Unit = {}): SimpleFieldConfig =
        field(
            "descriptor",
            ClassRef<TypeParameterRef>(TypeKind.Interface, Packages.descriptors, typeName),
            mutable = false,
            nullable = nullable,
            initializer = initializer
        )

    override val rootElement: ElementConfig by element(Other, name = "element") {
        +field("sourceSpan", type(Packages.tree, "SourceSpan")) {
            kdoc = """
            The span of source code of the syntax node from which this BIR node was generated,
            in number of characters from the start the source file. If there is no source information for this BIR node,
            the [SourceSpan.UNDEFINED] is used. In order to get the line number and the column number from this offset,
            [IrFileEntry.getLineNumber] and [IrFileEntry.getColumnNumber] can be used.
            
            @see IrFileEntry.getSourceRangeInfo
            """.trimIndent()
        }

        kDoc = "The root interface of the BIR tree. Each BIR node implements this interface."
    }
    val statement: ElementConfig by element(Other)

    val declaration: ElementConfig by element(Declaration) {
        parent(statement)
        parent(symbolOwner)
        parent(annotationContainerElement)

        +descriptor("DeclarationDescriptor")
        +field("origin", type("org.jetbrains.kotlin.ir.declarations", "IrDeclarationOrigin"))
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

        +field("isExternal", boolean)
    }
    val symbolOwner: ElementConfig by element(Declaration) {
        parent(type(Packages.symbols, "BirUntypedPossiblyElementSymbol"))

        +descriptor("DeclarationDescriptor")
        +field("signature", type("org.jetbrains.kotlin.ir.util", "IdSignature"), nullable = true) {
            generationCallback = {
                addModifiers(KModifier.OVERRIDE)
            }
        }
    }
    val metadataSourceOwner: ElementConfig by element(Declaration) {
        kDoc = """
        An [${elementName2typeName(rootElement.name)}] capable of holding something which backends can use to write
        as the metadata for the declaration.
        
        Technically, it can even be ± an array of bytes, but right now it's usually the frontend representation of the declaration,
        so a descriptor in case of K1, and [org.jetbrains.kotlin.fir.FirElement] in case of K2,
        and the backend invokes a metadata serializer on it to obtain metadata and write it, for example, to `@kotlin.Metadata`
        on JVM.
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
        +listField("overriddenSymbols", s, mutability = Var)
    }
    val memberWithContainerSource: ElementConfig by element(Declaration) {
        parent(declarationWithName)
    }
    val valueDeclaration: ElementConfig by element(Declaration) {
        parent(declarationWithName)
        parent(symbolOwner)

        +descriptor("ValueDescriptor")
        +symbol(SymbolTypes.value)
        +field("type", irTypeType)
        +field("isAssignable", boolean, mutable = false)
    }
    val valueParameter: ElementConfig by element(Declaration) {
        parent(declaration)
        parent(valueDeclaration)

        +descriptor("ParameterDescriptor")
        +symbol(SymbolTypes.valueParameter)
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
        parent(declaration)
        parent(possiblyExternalDeclaration)
        parent(declarationWithVisibility)
        parent(typeParametersContainer)
        parent(declarationContainer)
        parent(attributeContainer)
        parent(metadataSourceOwner)

        +descriptor("ClassDescriptor")
        +symbol(SymbolTypes.`class`)
        +field("kind", type<ClassKind>())
        +field("modality", type<Modality>())
        +field("isCompanion", boolean)
        +field("isInner", boolean)
        +field("isData", boolean)
        +field("isValue", boolean)
        +field("isExpect", boolean)
        +field("isFun", boolean)
        +field("hasEnumEntries", boolean) {
            kdoc = """
            Returns true iff this is a class loaded from dependencies which has the `HAS_ENUM_ENTRIES` metadata flag set.
            This flag is useful for Kotlin/JVM to determine whether an enum class from dependency actually has the `entries` property
            in its bytecode, as opposed to whether it has it in its member scope, which is true even for enum classes compiled by
            old versions of Kotlin which did not support the EnumEntries language feature.
            """.trimIndent()
        }
        +field("source", type<SourceElement>(), mutable = false)
        +listField("superTypes", irTypeType, mutability = Var)
        +field("thisReceiver", valueParameter, nullable = true, isChild = true)
        +field(
            "valueClassRepresentation",
            type<ValueClassRepresentation<*>>().withArgs(type(Packages.types, "BirSimpleType")),
            nullable = true,
        )
    }
    val attributeContainer: ElementConfig by element(Declaration) {
        kDoc = """
            Represents an IR element that can be copied, but must remember its original element. It is
            useful, for example, to keep track of generated names for anonymous declarations.
            @property attributeOwnerId original element before copying. Always satisfies the following
              invariant: `this.attributeOwnerId == this.attributeOwnerId.attributeOwnerId`.
        """.trimIndent()

        +field("attributeOwnerId", attributeContainer) {
            initializeToThis = true
        }
    }

    // Equivalent of IrMutableAnnotationContainer which is not an IR element (but could be)
    val annotationContainerElement: ElementConfig by element(Declaration) {
        parent(type(Packages.declarations, "BirAnnotationContainer"))

        +listField("annotations", constructorCall, mutability = Var) { // shouldn't those be child elements? rather not ref
            generationCallback = {
                addModifiers(KModifier.OVERRIDE)
            }
        }
    }
    val anonymousInitializer: ElementConfig by element(Declaration) {
        parent(declaration)

        +descriptor("ClassDescriptor") // TODO special descriptor for anonymous initializer blocks
        +symbol(SymbolTypes.anonymousInitializer)
        +field("isStatic", boolean)
        +field("body", blockBody, isChild = true)
    }
    val declarationContainer: ElementConfig by element(Declaration) {
        parent(declarationParent)

        +listField("declarations", declaration, mutability = List, isChild = true)
    }
    val typeParametersContainer: ElementConfig by element(Declaration) {
        parent(declaration)
        parent(declarationParent)

        +listField("typeParameters", typeParameter, mutability = Var, isChild = true)
    }
    val typeParameter: ElementConfig by element(Declaration) {
        parent(declaration)
        parent(declarationWithName)

        +descriptor("TypeParameterDescriptor")
        +symbol(SymbolTypes.typeParameter)
        +field("variance", type<Variance>())
        +field("index", int)
        +field("isReified", boolean)
        +listField("superTypes", irTypeType, mutability = Var)
    }
    val returnTarget: ElementConfig by element(Declaration) {
        parent(symbolOwner)

        +descriptor("FunctionDescriptor")
        +symbol(SymbolTypes.returnTarget)
    }
    val function: ElementConfig by element(Declaration) {
        parent(declaration)
        parent(possiblyExternalDeclaration)
        parent(declarationWithVisibility)
        parent(typeParametersContainer)
        parent(symbolOwner)
        parent(declarationParent)
        parent(returnTarget)
        parent(memberWithContainerSource)
        parent(metadataSourceOwner)

        +descriptor("FunctionDescriptor")
        +symbol(SymbolTypes.function)
        // NB: there's an inline constructor for Array and each primitive array class.
        +field("isInline", boolean)
        +field("isExpect", boolean)
        +field("returnType", irTypeType)
        +field("dispatchReceiverParameter", valueParameter, nullable = true, isChild = true)
        +field("extensionReceiverParameter", valueParameter, nullable = true, isChild = true)
        +listField("valueParameters", valueParameter, mutability = Var, isChild = true)
        // The first `contextReceiverParametersCount` value parameters are context receivers.
        +field("contextReceiverParametersCount", int)
        +field("body", body, nullable = true, isChild = true)
    }
    val constructor: ElementConfig by element(Declaration) {
        parent(function)

        +descriptor("ClassConstructorDescriptor")
        +symbol(SymbolTypes.constructor)
        +field("isPrimary", boolean)
    }
    val enumEntry: ElementConfig by element(Declaration) {
        parent(declaration)
        parent(declarationWithName)

        +descriptor("ClassDescriptor")
        +symbol(SymbolTypes.enumEntry)
        +field("initializerExpression", expressionBody, nullable = true, isChild = true)
        +field("correspondingClass", `class`, nullable = true, isChild = true)
    }
    val errorDeclaration: ElementConfig by element(Declaration) {
        parent(declaration)

        +symbol(symbolType)
    }
    val field: ElementConfig by element(Declaration) {
        parent(declaration)
        parent(possiblyExternalDeclaration)
        parent(declarationWithVisibility)
        parent(declarationParent)
        parent(metadataSourceOwner)

        +descriptor("PropertyDescriptor")
        +symbol(SymbolTypes.field)
        +field("type", irTypeType)
        +field("isFinal", boolean)
        +field("isStatic", boolean)
        +field("initializer", expressionBody, nullable = true, isChild = true)
        +field("correspondingPropertySymbol", SymbolTypes.property, nullable = true)
    }
    val localDelegatedProperty: ElementConfig by element(Declaration) {
        parent(declaration)
        parent(declarationWithName)
        parent(symbolOwner)
        parent(metadataSourceOwner)

        +descriptor("VariableDescriptorWithAccessors")
        +symbol(SymbolTypes.localDelegatedProperty)
        +field("type", irTypeType)
        +field("isVar", boolean)
        +field("delegate", variable, isChild = true)
        +field("getter", simpleFunction, isChild = true)
        +field("setter", simpleFunction, nullable = true, isChild = true)
    }
    val moduleFragment: ElementConfig by element(Declaration) {
        +descriptor("ModuleDescriptor")
        +field("name", type<Name>(), mutable = false)
        +listField("files", file, mutability = List, isChild = true)
    }
    val property: ElementConfig by element(Declaration) {
        isForcedLeaf = true

        parent(declaration)
        parent(possiblyExternalDeclaration)
        parent(overridableDeclaration.withArgs("S" to SymbolTypes.property))
        parent(metadataSourceOwner)
        parent(attributeContainer)
        parent(memberWithContainerSource)

        +descriptor("PropertyDescriptor")
        +symbol(SymbolTypes.property)
        +field("isVar", boolean)
        +field("isConst", boolean)
        +field("isLateinit", boolean)
        +field("isDelegated", boolean)
        +field("isExpect", boolean)
        +isFakeOverrideField()
        +field("backingField", field, nullable = true, isChild = true)
        +field("getter", simpleFunction, nullable = true, isChild = true)
        +field("setter", simpleFunction, nullable = true, isChild = true)
        +listField("overriddenSymbols", SymbolTypes.property, mutability = Var)
    }

    private fun isFakeOverrideField() = field("isFakeOverride", boolean) {
        code(
            "origin == %T.FAKE_OVERRIDE",
            type("org.jetbrains.kotlin.ir.declarations", "IrDeclarationOrigin").toPoet(),
        )
    }

    //TODO: make IrScript as IrPackageFragment, because script is used as a file, not as a class
    //NOTE: declarations and statements stored separately
    val script: ElementConfig by element(Declaration) {
        parent(declaration)
        parent(declarationWithName)
        parent(declarationParent)
        parent(statementContainer)
        parent(metadataSourceOwner)

        +symbol(SymbolTypes.script)
        // NOTE: is the result of the FE conversion, because there script interpreted as a class and has receiver
        // TODO: consider removing from here and handle appropriately in the lowering
        +field("thisReceiver", valueParameter, isChild = true, nullable = true) // K1
        +field("baseClass", irTypeType, nullable = true) // K1
        +listField("explicitCallParameters", variable, mutability = Var, isChild = true)
        +listField("implicitReceiversParameters", valueParameter, mutability = Var, isChild = true)
        +listField("providedProperties", SymbolTypes.property, mutability = Var)
        +listField("providedPropertiesParameters", valueParameter, mutability = Var, isChild = true)
        +field("resultProperty", SymbolTypes.property, nullable = true)
        +field("earlierScriptsParameter", valueParameter, nullable = true, isChild = true)
        +listField("importedScripts", SymbolTypes.script, mutability = Var, nullable = true)
        +listField("earlierScripts", SymbolTypes.script, mutability = Var, nullable = true)
        +field("targetClass", SymbolTypes.`class`, nullable = true)
        +field("constructor", constructor, nullable = true) // K1
    }
    val simpleFunction: ElementConfig by element(Declaration) {
        isForcedLeaf = true

        parent(function)
        parent(overridableDeclaration.withArgs("S" to SymbolTypes.simpleFunction))
        parent(attributeContainer)

        +symbol(SymbolTypes.simpleFunction)
        +field("isTailrec", boolean)
        +field("isSuspend", boolean)
        +isFakeOverrideField()
        +field("isOperator", boolean)
        +field("isInfix", boolean)
        +field("correspondingPropertySymbol", SymbolTypes.property, nullable = true)
        +listField("overriddenSymbols", SymbolTypes.simpleFunction, mutability = Var)
    }
    val typeAlias: ElementConfig by element(Declaration) {
        parent(declaration)
        parent(declarationWithName)
        parent(declarationWithVisibility)
        parent(typeParametersContainer)

        +descriptor("TypeAliasDescriptor")
        +symbol(SymbolTypes.typeAlias)
        +field("isActual", boolean)
        +field("expandedType", irTypeType)
    }
    val variable: ElementConfig by element(Declaration) {
        parent(declaration)
        parent(valueDeclaration)

        +descriptor("VariableDescriptor")
        +symbol(SymbolTypes.variable)
        +field("isVar", boolean)
        +field("isConst", boolean)
        +field("isLateinit", boolean)
        +field("initializer", expression, nullable = true, isChild = true)
    }
    val packageFragment: ElementConfig by element(Declaration) {
        parent(declarationContainer)
        parent(symbolOwner)

        +symbol(SymbolTypes.packageFragment)
        +descriptor("PackageFragmentDescriptor")
        +field("packageFqName", type<FqName>())
    }
    val externalPackageFragment: ElementConfig by element(Declaration) {
        parent(packageFragment)

        +symbol(SymbolTypes.externalPackageFragment)
        +field("containerSource", type<DeserializedContainerSource>(), nullable = true, mutable = false)
    }
    val file: ElementConfig by element(Declaration) {
        parent(packageFragment)
        parent(annotationContainerElement)
        parent(metadataSourceOwner)

        +symbol(SymbolTypes.file)
        +field("fileEntry", type("org.jetbrains.kotlin.ir", "IrFileEntry"))
    }

    val expression: ElementConfig by element(Expression) {
        parent(statement)
        parent(varargElement)
        parent(attributeContainer)

        +field("type", irTypeType)
    }
    val statementContainer: ElementConfig by element(Expression) {
        +listField("statements", statement, mutability = List, isChild = true)
    }
    val body: ElementConfig by element(Expression) {
        typeKind = TypeKind.Class
    }
    val expressionBody: ElementConfig by element(Expression) {
        parent(body)

        +field("expression", expression, isChild = true)
    }
    val blockBody: ElementConfig by element(Expression) {
        parent(body)
        parent(statementContainer)
    }
    val declarationReference: ElementConfig by element(Expression) {
        parent(expression)

        +symbol(symbolType)
    }
    val memberAccessExpression: ElementConfig by element(Expression) {
        val s = +param("S", symbolType)

        parent(declarationReference)

        +field("dispatchReceiver", expression, nullable = true, isChild = true)
        +field("extensionReceiver", expression, nullable = true, isChild = true)
        +symbol(s)
        +field("origin", statementOriginType, nullable = true)
        +listField("valueArguments", expression.copy(nullable = true), mutability = Array, isChild = true)
        +listField("typeArguments", irTypeType.copy(nullable = true), mutability = Var)
    }
    val functionAccessExpression: ElementConfig by element(Expression) {
        parent(memberAccessExpression.withArgs("S" to SymbolTypes.function))

        +field("contextReceiversCount", int)
    }
    val constructorCall: ElementConfig by element(Expression) {
        parent(functionAccessExpression)

        +symbol(SymbolTypes.constructor, mutable = true)
        +field("source", type<SourceElement>())
        +field("constructorTypeArgumentsCount", int)
    }
    val getSingletonValue: ElementConfig by element(Expression) {
        parent(declarationReference)
    }
    val getObjectValue: ElementConfig by element(Expression) {
        parent(getSingletonValue)

        +symbol(SymbolTypes.`class`, mutable = true)
    }
    val getEnumValue: ElementConfig by element(Expression) {
        parent(getSingletonValue)

        +symbol(SymbolTypes.enumEntry, mutable = true)
    }

    /**
     * Platform-specific low-level reference to function.
     *
     * On JS platform it represents a plain reference to JavaScript function.
     * On JVM platform it represents a MethodHandle constant.
     */
    val rawFunctionReference: ElementConfig by element(Expression) {
        parent(declarationReference)

        +symbol(SymbolTypes.function, mutable = true)
    }
    val containerExpression: ElementConfig by element(Expression) {
        parent(expression)
        parent(statementContainer)

        +field("origin", statementOriginType, nullable = true)
        +listField("statements", statement, mutability = List, isChild = true) {
            generationCallback = {
                addModifiers(KModifier.OVERRIDE)
            }
        }
    }
    val block: ElementConfig by element(Expression) {
        isForcedLeaf = true

        parent(containerExpression)
    }
    val composite: ElementConfig by element(Expression) {
        parent(containerExpression)
    }
    val returnableBlock: ElementConfig by element(Expression) {
        parent(block)
        parent(symbolOwner)
        parent(returnTarget)

        +symbol(SymbolTypes.returnableBlock)
    }
    val inlinedFunctionBlock: ElementConfig by element(Expression) {
        parent(block)

        +field("inlineCall", functionAccessExpression)
        +field("inlinedElement", rootElement)
    }
    val syntheticBody: ElementConfig by element(Expression) {
        parent(body)

        +field("kind", type("org.jetbrains.kotlin.ir.expressions", "IrSyntheticBodyKind"))
    }
    val breakContinue: ElementConfig by element(Expression) {
        parent(expression)

        +field("loop", loop)
        +field("label", string, nullable = true)
    }
    val `break` by element(Expression) {
        parent(breakContinue)
    }
    val `continue` by element(Expression) {
        parent(breakContinue)
    }
    val call: ElementConfig by element(Expression) {
        parent(functionAccessExpression)

        +symbol(SymbolTypes.simpleFunction, mutable = true)
        +field("superQualifierSymbol", SymbolTypes.`class`, nullable = true)
    }
    val callableReference: ElementConfig by element(Expression) {
        val s = +param("S", symbolType)

        parent(memberAccessExpression.withArgs("S" to s))

        +symbol(s, mutable = true)
    }
    val functionReference: ElementConfig by element(Expression) {
        parent(callableReference.withArgs("S" to SymbolTypes.function))

        +symbol(SymbolTypes.function, mutable = true)
        +field("reflectionTarget", SymbolTypes.function, nullable = true)
    }
    val propertyReference: ElementConfig by element(Expression) {
        parent(callableReference.withArgs("S" to SymbolTypes.property))

        +symbol(SymbolTypes.property, mutable = true)
        +field("field", SymbolTypes.field, nullable = true)
        +field("getter", SymbolTypes.simpleFunction, nullable = true)
        +field("setter", SymbolTypes.simpleFunction, nullable = true)
    }
    val localDelegatedPropertyReference: ElementConfig by element(Expression) {
        parent(callableReference.withArgs("S" to SymbolTypes.localDelegatedProperty))

        +symbol(SymbolTypes.localDelegatedProperty, mutable = true)
        +field("delegate", SymbolTypes.variable)
        +field("getter", simpleFunction)
        +field("setter", simpleFunction, nullable = true)
    }
    val classReference: ElementConfig by element(Expression) {
        parent(declarationReference)

        +symbol(SymbolTypes.classifier, mutable = true)
        +field("classType", irTypeType)
    }
    val const: ElementConfig by element(Expression) {
        val t = +param("T")

        parent(expression)

        +field("kind", type("org.jetbrains.kotlin.ir.expressions", "IrConstKind").withArgs(t))
        +field("value", t)
    }
    val constantValue: ElementConfig by element(Expression) {
        parent(expression)
    }
    val constantPrimitive: ElementConfig by element(Expression) {
        parent(constantValue)

        +field("value", const.withArgs("T" to TypeRef.Star), isChild = true)
    }
    val constantObject: ElementConfig by element(Expression) {
        parent(constantValue)

        +field("constructor", SymbolTypes.constructor)
        +listField("valueArguments", constantValue, mutability = List, isChild = true)
        +listField("typeArguments", irTypeType, mutability = Var)
    }
    val constantArray: ElementConfig by element(Expression) {
        parent(constantValue)

        +listField("elements", constantValue, mutability = List, isChild = true)
    }
    val delegatingConstructorCall: ElementConfig by element(Expression) {
        parent(functionAccessExpression)

        +symbol(SymbolTypes.constructor, mutable = true)
    }
    val dynamicExpression: ElementConfig by element(Expression) {
        parent(expression)
    }
    val dynamicOperatorExpression: ElementConfig by element(Expression) {
        parent(dynamicExpression)

        +field("operator", type("org.jetbrains.kotlin.ir.expressions", "IrDynamicOperator"))
        +field("receiver", expression, isChild = true)
        +listField("arguments", expression, mutability = List, isChild = true)
    }
    val dynamicMemberExpression: ElementConfig by element(Expression) {
        parent(dynamicExpression)

        +field("memberName", string)
        +field("receiver", expression, isChild = true)
    }
    val enumConstructorCall: ElementConfig by element(Expression) {
        parent(functionAccessExpression)

        +symbol(SymbolTypes.constructor, mutable = true)
    }
    val errorExpression: ElementConfig by element(Expression) {
        isForcedLeaf = true

        parent(expression)

        +field("description", string)
    }
    val errorCallExpression: ElementConfig by element(Expression) {
        parent(errorExpression)

        +field("explicitReceiver", expression, nullable = true, isChild = true)
        +listField("arguments", expression, mutability = List, isChild = true)
    }
    val fieldAccessExpression: ElementConfig by element(Expression) {
        parent(declarationReference)

        +symbol(SymbolTypes.field, mutable = true)
        +field("superQualifierSymbol", SymbolTypes.`class`, nullable = true)
        +field("receiver", expression, nullable = true, isChild = true)
        +field("origin", statementOriginType, nullable = true)
    }
    val getField: ElementConfig by element(Expression) {
        parent(fieldAccessExpression)
    }
    val setField: ElementConfig by element(Expression) {
        parent(fieldAccessExpression)

        +field("value", expression, isChild = true)
    }
    val functionExpression: ElementConfig by element(Expression) {
        parent(expression)

        +field("origin", statementOriginType)
        +field("function", simpleFunction, isChild = true)
    }
    val getClass: ElementConfig by element(Expression) {
        parent(expression)

        +field("argument", expression, isChild = true)
    }
    val instanceInitializerCall: ElementConfig by element(Expression) {
        parent(expression)

        +field("classSymbol", SymbolTypes.`class`)
    }
    val loop: ElementConfig by element(Expression) {
        parent(expression)

        +field("origin", statementOriginType, nullable = true)
        +field("body", expression, nullable = true, isChild = true)
        +field("condition", expression, isChild = true)
        +field("label", string, nullable = true)
    }
    val whileLoop: ElementConfig by element(Expression) {
        childrenOrderOverride = listOf("condition", "body")

        parent(loop)
    }
    val doWhileLoop: ElementConfig by element(Expression) {
        parent(loop)
    }
    val `return`: ElementConfig by element(Expression) {
        parent(expression)

        +field("value", expression, isChild = true)
        +field("returnTargetSymbol", SymbolTypes.returnTarget)
    }
    val stringConcatenation: ElementConfig by element(Expression) {
        parent(expression)

        +listField("arguments", expression, mutability = List, isChild = true)
    }
    val suspensionPoint: ElementConfig by element(Expression) {
        parent(expression)

        +field("suspensionPointIdParameter", variable, isChild = true)
        +field("result", expression, isChild = true)
        +field("resumeResult", expression, isChild = true)
    }
    val suspendableExpression: ElementConfig by element(Expression) {
        parent(expression)

        +field("suspensionPointId", expression, isChild = true)
        +field("result", expression, isChild = true)
    }
    val `throw`: ElementConfig by element(Expression) {
        parent(expression)

        +field("value", expression, isChild = true)
    }
    val `try`: ElementConfig by element(Expression) {
        parent(expression)

        +field("tryResult", expression, isChild = true)
        +listField("catches", catch, mutability = List, isChild = true)
        +field("finallyExpression", expression, nullable = true, isChild = true)
    }
    val catch: ElementConfig by element(Expression) {
        +field("catchParameter", variable, isChild = true)
        +field("result", expression, isChild = true)
    }
    val typeOperatorCall: ElementConfig by element(Expression) {
        parent(expression)

        +field("operator", type("org.jetbrains.kotlin.ir.expressions", "IrTypeOperator"))
        +field("argument", expression, isChild = true)
        +field("typeOperand", irTypeType)
    }
    val valueAccessExpression: ElementConfig by element(Expression) {
        parent(declarationReference)

        +symbol(valueDeclaration, mutable = true)
        +field("origin", statementOriginType, nullable = true)
    }
    val getValue: ElementConfig by element(Expression) {
        parent(valueAccessExpression)
    }
    val setValue: ElementConfig by element(Expression) {
        parent(valueAccessExpression)

        +field("value", expression, isChild = true)
    }
    val varargElement: ElementConfig by element(Expression)
    val vararg: ElementConfig by element(Expression) {
        parent(expression)

        +field("varargElementType", irTypeType)
        +listField("elements", varargElement, mutability = List, isChild = true)
    }
    val spreadElement: ElementConfig by element(Expression) {
        parent(varargElement)

        +field("expression", expression, isChild = true)
    }
    val `when`: ElementConfig by element(Expression) {
        parent(expression)

        +field("origin", statementOriginType, nullable = true)
        +listField("branches", branch, mutability = List, isChild = true)
    }
    val branch: ElementConfig by element(Expression) {
        isForcedLeaf = true

        +field("condition", expression, isChild = true)
        +field("result", expression, isChild = true)
    }
    val elseBranch: ElementConfig by element(Expression) {
        parent(branch)
    }
}
