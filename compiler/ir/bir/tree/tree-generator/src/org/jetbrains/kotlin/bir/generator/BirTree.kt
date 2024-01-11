
/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.generator

import org.jetbrains.kotlin.bir.generator.config.AbstractTreeBuilder
import org.jetbrains.kotlin.bir.generator.model.Element
import org.jetbrains.kotlin.bir.generator.model.Element.Category.*
import org.jetbrains.kotlin.bir.generator.model.ListField.Mutability.*
import org.jetbrains.kotlin.bir.generator.model.ListField.Mutability.Array
import org.jetbrains.kotlin.bir.generator.model.ListField.Mutability.List
import org.jetbrains.kotlin.bir.generator.model.SingleField
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
// 1) config (see properties of Element)
// 2) parents
// 3) fields
object BirTree : AbstractTreeBuilder() {
    private fun symbol(type: TypeRefWithNullability, mutable: Boolean = false): SingleField =
        field("symbol", type, mutable = mutable, isChild = false)

    override val rootElement: Element by element(Other, name = "Element") {
        parent(type(Packages.tree, "BirElementFacade"))

        +field("sourceSpan", type(Packages.tree, "CompressedSourceSpan")) {
            kDoc = """
            The span of source code of the syntax node from which this BIR node was generated,
            in number of characters from the start the source file. If there is no source information for this BIR node,
            the [SourceSpan.UNDEFINED] is used. In order to get the line number and the column number from this offset,
            [IrFileEntry.getLineNumber] and [IrFileEntry.getColumnNumber] can be used.
            
            @see IrFileEntry.getSourceRangeInfo
            """.trimIndent()
        }

        kDoc = "The root interface of the BIR tree. Each BIR node implements this interface."
    }
    val statement: Element by element(Other)

    val declaration: Element by element(Declaration) {
        parent(statement)
        parent(symbolOwner)
        parent(annotationContainerElement)

        +field("origin", type("org.jetbrains.kotlin.ir.declarations", "IrDeclarationOrigin"))
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

        +field("isExternal", boolean)
    }
    val symbolOwner: Element by element(Declaration) {
        parent(type(Packages.declarations, "BirSymbolOwnerFacade"))

        +field("signature", type("org.jetbrains.kotlin.ir.util", "IdSignature"), nullable = true) {
            isOverride = true
        }
    }
    val metadataSourceOwner: Element by element(Declaration) {
        kDoc = """
        An [${rootElement.typeName}] capable of holding something which backends can use to write
        as the metadata for the declaration.
        
        Technically, it can even be ± an array of bytes, but right now it's usually the frontend representation of the declaration,
        so a descriptor in case of K1, and [org.jetbrains.kotlin.fir.FirElement] in case of K2,
        and the backend invokes a metadata serializer on it to obtain metadata and write it, for example, to `@kotlin.Metadata`
        on JVM.
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
        +listField("overriddenSymbols", s, mutability = Var)
    }
    val memberWithContainerSource: Element by element(Declaration) {
        parent(declarationWithName)
    }
    val valueDeclaration: Element by element(Declaration) {
        parent(declarationWithName)
        parent(symbolOwner)

        +symbol(SymbolTypes.value)
        +field("type", irTypeType)
        +field("isAssignable", boolean)
    }
    val valueParameter: Element by element(Declaration) {
        typeKind = TypeKind.Interface

        parent(declaration)
        parent(valueDeclaration)

        +symbol(SymbolTypes.valueParameter)
        +field("index", int)
        +field("varargElementType", irTypeType, nullable = true)
        +field("isCrossinline", boolean)
        +field("isNoinline", boolean)
        +field("isHidden", boolean) {
            //additionalImports.add(ArbitraryImportable("org.jetbrains.kotlin.ir.util", "IdSignature"))
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
        typeKind = TypeKind.Interface

        parent(declaration)
        parent(possiblyExternalDeclaration)
        parent(declarationWithVisibility)
        parent(typeParametersContainer)
        parent(declarationContainer)
        parent(attributeContainer)
        parent(metadataSourceOwner)

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
            kDoc = """
            Returns true iff this is a class loaded from dependencies which has the `HAS_ENUM_ENTRIES` metadata flag set.
            This flag is useful for Kotlin/JVM to determine whether an enum class from dependency actually has the `entries` property
            in its bytecode, as opposed to whether it has it in its member scope, which is true even for enum classes compiled by
            old versions of Kotlin which did not support the EnumEntries language feature.
            """.trimIndent()
        }
        +field("source", type<SourceElement>(), mutable = false)
        +listField("superTypes", irTypeType, mutability = Var)
        +field("thisReceiver", valueParameter, nullable = true)
        +field(
            "valueClassRepresentation",
            type<ValueClassRepresentation<*>>().withArgs(type(Packages.types, "BirSimpleType")),
            nullable = true,
        )
    }
    val attributeContainer: Element by element(Declaration) {
        kDoc = """
            Represents an IR element that can be copied, but must remember its original element. It is
            useful, for example, to keep track of generated names for anonymous declarations.
            @property attributeOwnerId original element before copying. Always satisfies the following
              invariant: `this.attributeOwnerId == this.attributeOwnerId.attributeOwnerId`.
        """.trimIndent()

        +field("attributeOwnerId", attributeContainer, isChild = false) {
            initializeToThis = true
        }
    }

    // Equivalent of IrMutableAnnotationContainer which is not an IR element (but could be)
    val annotationContainerElement: Element by element(Declaration) {
        parent(type(Packages.declarations, "BirAnnotationContainer"))

        +listField("annotations", constructorCall, mutability = Var) { // shouldn't those be child elements? rather not ref
            isOverride = true
        }
    }
    val anonymousInitializer: Element by element(Declaration) {
        parent(declaration)

        +symbol(SymbolTypes.anonymousInitializer)
        +field("isStatic", boolean)
        +field("body", blockBody, nullable = true)
    }
    val declarationContainer: Element by element(Declaration) {
        parent(declarationParent)

        +listField("declarations", declaration, mutability = List)
    }
    val typeParametersContainer: Element by element(Declaration) {
        parent(declaration)
        parent(declarationParent)

        +listField("typeParameters", typeParameter, mutability = Var)
    }
    val typeParameter: Element by element(Declaration) {
        parent(declaration)
        parent(declarationWithName)

        +symbol(SymbolTypes.typeParameter)
        +field("variance", type<Variance>())
        +field("index", int)
        +field("isReified", boolean)
        +listField("superTypes", irTypeType, mutability = Var)
    }
    val returnTarget: Element by element(Declaration) {
        parent(symbolOwner)

        +symbol(SymbolTypes.returnTarget)
    }
    val function: Element by element(Declaration) {
        parent(declaration)
        parent(possiblyExternalDeclaration)
        parent(declarationWithVisibility)
        parent(typeParametersContainer)
        parent(symbolOwner)
        parent(declarationParent)
        parent(returnTarget)
        parent(memberWithContainerSource)
        parent(metadataSourceOwner)

        +symbol(SymbolTypes.function)
        // NB: there's an inline constructor for Array and each primitive array class.
        +field("isInline", boolean)
        +field("isExpect", boolean)
        +field("returnType", irTypeType)
        +field("dispatchReceiverParameter", valueParameter, nullable = true)
        +field("extensionReceiverParameter", valueParameter, nullable = true)
        +listField("valueParameters", valueParameter, mutability = Var)
        // The first `contextReceiverParametersCount` value parameters are context receivers.
        +field("contextReceiverParametersCount", int)
        +field("body", body, nullable = true)
    }
    val constructor: Element by element(Declaration) {
        typeKind = TypeKind.Interface

        parent(function)

        +symbol(SymbolTypes.constructor)
        +field("isPrimary", boolean)
    }
    val enumEntry: Element by element(Declaration) {
        typeKind = TypeKind.Interface

        parent(declaration)
        parent(declarationWithName)

        +symbol(SymbolTypes.enumEntry)
        +field("initializerExpression", expressionBody, nullable = true)
        +field("correspondingClass", `class`, nullable = true)
    }
    val errorDeclaration: Element by element(Declaration) {
        parent(declaration)

        +symbol(symbolType)
    }
    val field: Element by element(Declaration) {
        typeKind = TypeKind.Interface

        parent(declaration)
        parent(possiblyExternalDeclaration)
        parent(declarationWithVisibility)
        parent(declarationParent)
        parent(metadataSourceOwner)

        +symbol(SymbolTypes.field)
        +field("type", irTypeType)
        +field("isFinal", boolean)
        +field("isStatic", boolean)
        +field("initializer", expressionBody, nullable = true)
        +field("correspondingPropertySymbol", SymbolTypes.property, nullable = true)
    }
    val localDelegatedProperty: Element by element(Declaration) {
        parent(declaration)
        parent(declarationWithName)
        parent(symbolOwner)
        parent(metadataSourceOwner)

        +symbol(SymbolTypes.localDelegatedProperty)
        +field("type", irTypeType)
        +field("isVar", boolean)
        +field("delegate", variable, nullable = true)
        +field("getter", simpleFunction, nullable = true)
        +field("setter", simpleFunction, nullable = true)
    }
    val moduleFragment: Element by element(Declaration) {
        +field("name", type<Name>(), mutable = false)
        +field("descriptor", type(Packages.descriptors, "ModuleDescriptor"), mutable = false) {
            optInAnnotation = obsoleteDescriptorBasedApiAnnotation
        }
        +listField("files", file, mutability = List)
    }
    val property: Element by element(Declaration) {
        typeKind = TypeKind.Interface
        isLeaf = true

        parent(declaration)
        parent(possiblyExternalDeclaration)
        parent(overridableDeclaration.withArgs("S" to SymbolTypes.property))
        parent(metadataSourceOwner)
        parent(attributeContainer)
        parent(memberWithContainerSource)

        +symbol(SymbolTypes.property)
        +field("isVar", boolean)
        +field("isConst", boolean)
        +field("isLateinit", boolean)
        +field("isDelegated", boolean)
        +field("isExpect", boolean)
        +isFakeOverrideField()
        +field("backingField", field, nullable = true)
        +field("getter", simpleFunction, nullable = true)
        +field("setter", simpleFunction, nullable = true)
        +listField("overriddenSymbols", SymbolTypes.property, mutability = Var)
    }

    private fun isFakeOverrideField() = field("isFakeOverride", boolean)

    //TODO: make IrScript as IrPackageFragment, because script is used as a file, not as a class
    //NOTE: declarations and statements stored separately
    val script: Element by element(Declaration) {
        parent(declaration)
        parent(declarationWithName)
        parent(declarationParent)
        parent(statementContainer)
        parent(metadataSourceOwner)

        +symbol(SymbolTypes.script)
        // NOTE: is the result of the FE conversion, because there script interpreted as a class and has receiver
        // TODO: consider removing from here and handle appropriately in the lowering
        +field("thisReceiver", valueParameter, nullable = true) // K1
        +field("baseClass", irTypeType, nullable = true) // K1
        +listField("explicitCallParameters", variable, mutability = Var)
        +listField("implicitReceiversParameters", valueParameter, mutability = Var)
        +listField("providedProperties", SymbolTypes.property, mutability = Var)
        +listField("providedPropertiesParameters", valueParameter, mutability = Var)
        +field("resultProperty", SymbolTypes.property, nullable = true)
        +field("earlierScriptsParameter", valueParameter, nullable = true)
        +listField("importedScripts", SymbolTypes.script, nullable = true, mutability = Var)
        +listField("earlierScripts", SymbolTypes.script, nullable = true, mutability = Var)
        +field("targetClass", SymbolTypes.`class`, nullable = true)
        +field("constructor", constructor, nullable = true, isChild = false) // K1
    }
    val simpleFunction: Element by element(Declaration) {
        isLeaf = true
        typeKind = TypeKind.Interface

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
    val typeAlias: Element by element(Declaration) {
        typeKind = TypeKind.Interface

        parent(declaration)
        parent(declarationWithName)
        parent(declarationWithVisibility)
        parent(typeParametersContainer)

        +symbol(SymbolTypes.typeAlias)
        +field("isActual", boolean)
        +field("expandedType", irTypeType)
    }
    val variable: Element by element(Declaration) {
        parent(declaration)
        parent(valueDeclaration)

        +symbol(SymbolTypes.variable)
        +field("isVar", boolean)
        +field("isConst", boolean)
        +field("isLateinit", boolean)
        +field("initializer", expression, nullable = true)
    }
    val packageFragment: Element by element(Declaration) {
        parent(declarationContainer)
        parent(symbolOwner)

        +symbol(SymbolTypes.packageFragment)
        +field("packageFqName", type<FqName>())
    }
    val externalPackageFragment: Element by element(Declaration) {
        parent(packageFragment)

        +symbol(SymbolTypes.externalPackageFragment)
        +field("containerSource", type<DeserializedContainerSource>(), nullable = true, mutable = false)
    }
    val file: Element by element(Declaration) {
        parent(packageFragment)
        parent(annotationContainerElement)
        parent(metadataSourceOwner)

        +symbol(SymbolTypes.file)
        +field("fileEntry", type("org.jetbrains.kotlin.ir", "IrFileEntry"))
    }

    val expression: Element by element(Expression) {
        parent(statement)
        parent(varargElement)
        parent(attributeContainer)

        +field("type", irTypeType)
    }
    val statementContainer: Element by element(Expression) {
        +listField("statements", statement, mutability = List)
    }
    val body: Element by element(Expression) {
        typeKind = TypeKind.Class
    }
    val expressionBody: Element by element(Expression) {
        parent(body)

        +field("expression", expression, nullable = true)
    }
    val blockBody: Element by element(Expression) {
        parent(body)
        parent(statementContainer)
    }
    val declarationReference: Element by element(Expression) {
        parent(expression)

        +symbol(symbolType)
    }
    val memberAccessExpression: Element by element(Expression) {
        val s = +param("S", symbolType)

        parent(declarationReference)

        +field("dispatchReceiver", expression, nullable = true)
        +field("extensionReceiver", expression, nullable = true)
        +symbol(s)
        +field("origin", statementOriginType, nullable = true)
        +listField("valueArguments", expression.copy(nullable = true), mutability = Array)
        +listField("typeArguments", irTypeType.copy(nullable = true), mutability = Var)
    }
    val functionAccessExpression: Element by element(Expression) {
        parent(memberAccessExpression.withArgs("S" to SymbolTypes.function))

        +field("contextReceiversCount", int)
    }
    val constructorCall: Element by element(Expression) {
        parent(functionAccessExpression)

        +symbol(SymbolTypes.constructor, mutable = true)
        +field("source", type<SourceElement>())
        +field("constructorTypeArgumentsCount", int)
    }
    val getSingletonValue: Element by element(Expression) {
        parent(declarationReference)
    }
    val getObjectValue: Element by element(Expression) {
        parent(getSingletonValue)

        +symbol(SymbolTypes.`class`, mutable = true)
    }
    val getEnumValue: Element by element(Expression) {
        parent(getSingletonValue)

        +symbol(SymbolTypes.enumEntry, mutable = true)
    }

    /**
     * Platform-specific low-level reference to function.
     *
     * On JS platform it represents a plain reference to JavaScript function.
     * On JVM platform it represents a MethodHandle constant.
     */
    val rawFunctionReference: Element by element(Expression) {
        parent(declarationReference)

        +symbol(SymbolTypes.function, mutable = true)
    }
    val containerExpression: Element by element(Expression) {
        parent(expression)
        parent(statementContainer)

        +field("origin", statementOriginType, nullable = true)
        +listField("statements", statement, mutability = List) {
            isOverride = true
        }
    }
    val block: Element by element(Expression) {
        isLeaf = true

        parent(containerExpression)
    }
    val composite: Element by element(Expression) {
        parent(containerExpression)
    }
    val returnableBlock: Element by element(Expression) {
        parent(block)
        parent(symbolOwner)
        parent(returnTarget)

        +symbol(SymbolTypes.returnableBlock)
    }
    val inlinedFunctionBlock: Element by element(Expression) {
        parent(block)

        +field("inlineCall", functionAccessExpression, isChild = false)
        +field("inlinedElement", rootElement, isChild = false)
    }
    val syntheticBody: Element by element(Expression) {
        parent(body)

        +field("kind", type("org.jetbrains.kotlin.ir.expressions", "IrSyntheticBodyKind"))
    }
    val breakContinue: Element by element(Expression) {
        parent(expression)

        +field("loop", loop, isChild = false)
        +field("label", string, nullable = true)
    }
    val `break` by element(Expression) {
        parent(breakContinue)
    }
    val `continue` by element(Expression) {
        parent(breakContinue)
    }
    val call: Element by element(Expression) {
        parent(functionAccessExpression)

        +symbol(SymbolTypes.simpleFunction, mutable = true)
        +field("superQualifierSymbol", SymbolTypes.`class`, nullable = true)
    }
    val callableReference: Element by element(Expression) {
        val s = +param("S", symbolType)

        parent(memberAccessExpression.withArgs("S" to s))

        +symbol(s, mutable = true)
    }
    val functionReference: Element by element(Expression) {
        parent(callableReference.withArgs("S" to SymbolTypes.function))

        +symbol(SymbolTypes.function, mutable = true)
        +field("reflectionTarget", SymbolTypes.function, nullable = true)
    }
    val propertyReference: Element by element(Expression) {
        parent(callableReference.withArgs("S" to SymbolTypes.property))

        +symbol(SymbolTypes.property, mutable = true)
        +field("field", SymbolTypes.field, nullable = true)
        +field("getter", SymbolTypes.simpleFunction, nullable = true)
        +field("setter", SymbolTypes.simpleFunction, nullable = true)
    }
    val localDelegatedPropertyReference: Element by element(Expression) {
        parent(callableReference.withArgs("S" to SymbolTypes.localDelegatedProperty))

        +symbol(SymbolTypes.localDelegatedProperty, mutable = true)
        +field("delegate", SymbolTypes.variable)
        +field("getter", simpleFunction, isChild = false)
        +field("setter", simpleFunction, nullable = true, isChild = false)
    }
    val classReference: Element by element(Expression) {
        parent(declarationReference)

        +symbol(SymbolTypes.classifier, mutable = true)
        +field("classType", irTypeType)
    }
    val const: Element by element(Expression) {
        val t = +param("T")

        parent(expression)

        +field("kind", type("org.jetbrains.kotlin.ir.expressions", "IrConstKind").withArgs(t))
        +field("value", t)
    }
    val constantValue: Element by element(Expression) {
        parent(expression)
    }
    val constantPrimitive: Element by element(Expression) {
        parent(constantValue)

        +field("value", const.withArgs("T" to TypeRef.Star), nullable = true)
    }
    val constantObject: Element by element(Expression) {
        parent(constantValue)

        +field("constructor", SymbolTypes.constructor)
        +listField("valueArguments", constantValue, mutability = List)
        +listField("typeArguments", irTypeType, mutability = Var)
    }
    val constantArray: Element by element(Expression) {
        parent(constantValue)

        +listField("elements", constantValue, mutability = List)
    }
    val delegatingConstructorCall: Element by element(Expression) {
        parent(functionAccessExpression)

        +symbol(SymbolTypes.constructor, mutable = true)
    }
    val dynamicExpression: Element by element(Expression) {
        parent(expression)
    }
    val dynamicOperatorExpression: Element by element(Expression) {
        parent(dynamicExpression)

        +field("operator", type("org.jetbrains.kotlin.ir.expressions", "IrDynamicOperator"))
        +field("receiver", expression, nullable = true)
        +listField("arguments", expression, mutability = List)
    }
    val dynamicMemberExpression: Element by element(Expression) {
        parent(dynamicExpression)

        +field("memberName", string)
        +field("receiver", expression, nullable = true)
    }
    val enumConstructorCall: Element by element(Expression) {
        parent(functionAccessExpression)

        +symbol(SymbolTypes.constructor, mutable = true)
    }
    val errorExpression: Element by element(Expression) {
        isLeaf = true

        parent(expression)

        +field("description", string)
    }
    val errorCallExpression: Element by element(Expression) {
        parent(errorExpression)

        +field("explicitReceiver", expression, nullable = true)
        +listField("arguments", expression, mutability = List)
    }
    val fieldAccessExpression: Element by element(Expression) {
        parent(declarationReference)

        +symbol(SymbolTypes.field, mutable = true)
        +field("superQualifierSymbol", SymbolTypes.`class`, nullable = true)
        +field("receiver", expression, nullable = true)
        +field("origin", statementOriginType, nullable = true)
    }
    val getField: Element by element(Expression) {
        parent(fieldAccessExpression)
    }
    val setField: Element by element(Expression) {
        parent(fieldAccessExpression)

        +field("value", expression, nullable = true)
    }
    val functionExpression: Element by element(Expression) {
        parent(expression)

        +field("origin", statementOriginType)
        +field("function", simpleFunction, nullable = true)
    }
    val getClass: Element by element(Expression) {
        parent(expression)

        +field("argument", expression, nullable = true)
    }
    val instanceInitializerCall: Element by element(Expression) {
        parent(expression)

        +field("classSymbol", SymbolTypes.`class`)
    }
    val loop: Element by element(Expression) {
        parent(expression)

        +field("origin", statementOriginType, nullable = true)
        +field("body", expression, nullable = true)
        +field("condition", expression, nullable = true)
        +field("label", string, nullable = true)
    }
    val whileLoop: Element by element(Expression) {
        childrenOrderOverride = listOf("condition", "body")

        parent(loop)
    }
    val doWhileLoop: Element by element(Expression) {
        parent(loop)
    }
    val `return`: Element by element(Expression) {
        parent(expression)

        +field("value", expression, nullable = true)
        +field("returnTargetSymbol", SymbolTypes.returnTarget)
    }
    val stringConcatenation: Element by element(Expression) {
        parent(expression)

        +listField("arguments", expression, mutability = List)
    }
    val suspensionPoint: Element by element(Expression) {
        parent(expression)

        +field("suspensionPointIdParameter", variable, nullable = true)
        +field("result", expression, nullable = true)
        +field("resumeResult", expression, nullable = true)
    }
    val suspendableExpression: Element by element(Expression) {
        parent(expression)

        +field("suspensionPointId", expression, nullable = true)
        +field("result", expression, nullable = true)
    }
    val `throw`: Element by element(Expression) {
        parent(expression)

        +field("value", expression, nullable = true)
    }
    val `try`: Element by element(Expression) {
        parent(expression)

        +field("tryResult", expression, nullable = true)
        +listField("catches", catch, mutability = List)
        +field("finallyExpression", expression, nullable = true)
    }
    val catch: Element by element(Expression) {
        +field("catchParameter", variable, nullable = true)
        +field("result", expression, nullable = true)
    }
    val typeOperatorCall: Element by element(Expression) {
        parent(expression)

        +field("operator", type("org.jetbrains.kotlin.ir.expressions", "IrTypeOperator"))
        +field("argument", expression, nullable = true)
        +field("typeOperand", irTypeType)
    }
    val valueAccessExpression: Element by element(Expression) {
        parent(declarationReference)

        +symbol(valueDeclaration, mutable = true)
        +field("origin", statementOriginType, nullable = true)
    }
    val getValue: Element by element(Expression) {
        parent(valueAccessExpression)
    }
    val setValue: Element by element(Expression) {
        parent(valueAccessExpression)

        +field("value", expression, nullable = true)
    }
    val varargElement: Element by element(Expression)
    val vararg: Element by element(Expression) {
        parent(expression)

        +field("varargElementType", irTypeType)
        +listField("elements", varargElement, mutability = List)
    }
    val spreadElement: Element by element(Expression) {
        parent(varargElement)

        +field("expression", expression, nullable = true)
    }
    val `when`: Element by element(Expression) {
        parent(expression)

        +field("origin", statementOriginType, nullable = true)
        +listField("branches", branch, mutability = List)
    }
    val branch: Element by element(Expression) {
        isLeaf = true

        +field("condition", expression, nullable = true)
        +field("result", expression, nullable = true)
    }
    val elseBranch: Element by element(Expression) {
        parent(branch)
    }
}
