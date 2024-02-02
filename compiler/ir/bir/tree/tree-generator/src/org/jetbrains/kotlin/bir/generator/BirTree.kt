/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.generator

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.ValueClassRepresentation
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.*
import org.jetbrains.kotlin.bir.generator.BirSymbolTree.anonymousInitializerSymbol
import org.jetbrains.kotlin.bir.generator.BirSymbolTree.classSymbol
import org.jetbrains.kotlin.bir.generator.BirSymbolTree.classifierSymbol
import org.jetbrains.kotlin.bir.generator.BirSymbolTree.constructorSymbol
import org.jetbrains.kotlin.bir.generator.BirSymbolTree.enumEntrySymbol
import org.jetbrains.kotlin.bir.generator.BirSymbolTree.externalPackageFragmentSymbol
import org.jetbrains.kotlin.bir.generator.BirSymbolTree.fieldSymbol
import org.jetbrains.kotlin.bir.generator.BirSymbolTree.fileSymbol
import org.jetbrains.kotlin.bir.generator.BirSymbolTree.functionSymbol
import org.jetbrains.kotlin.bir.generator.BirSymbolTree.localDelegatedPropertySymbol
import org.jetbrains.kotlin.bir.generator.BirSymbolTree.packageFragmentSymbol
import org.jetbrains.kotlin.bir.generator.BirSymbolTree.propertySymbol
import org.jetbrains.kotlin.bir.generator.BirSymbolTree.returnTargetSymbol
import org.jetbrains.kotlin.bir.generator.BirSymbolTree.returnableBlockSymbol
import org.jetbrains.kotlin.bir.generator.BirSymbolTree.scriptSymbol
import org.jetbrains.kotlin.bir.generator.BirSymbolTree.simpleFunctionSymbol
import org.jetbrains.kotlin.bir.generator.BirSymbolTree.typeAliasSymbol
import org.jetbrains.kotlin.bir.generator.BirSymbolTree.typeParameterSymbol
import org.jetbrains.kotlin.bir.generator.BirSymbolTree.valueParameterSymbol
import org.jetbrains.kotlin.bir.generator.BirSymbolTree.valueSymbol
import org.jetbrains.kotlin.bir.generator.BirSymbolTree.variableSymbol
import org.jetbrains.kotlin.bir.generator.config.AbstractTreeBuilder
import org.jetbrains.kotlin.bir.generator.model.Element
import org.jetbrains.kotlin.bir.generator.model.Element.Category.*
import org.jetbrains.kotlin.bir.generator.model.ListField.Mutability.*
import org.jetbrains.kotlin.bir.generator.model.SingleField
import org.jetbrains.kotlin.bir.generator.model.symbol.Symbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.AnnotationMarker

// Note the style of the DSL to describe IR elements, which is these things in the following order:
// 1) config (see properties of Element)
// 2) parents
// 3) fields
object BirTree : AbstractTreeBuilder() {

    private fun descriptor(typeName: String, nullable: Boolean = false): SingleField =
        field(
            name = "descriptor",
            type = type(IrPackages.descriptors, typeName),
            mutable = false,
            nullable = nullable,
        ) {
            optInAnnotation = obsoleteDescriptorBasedApiAnnotation
            skipInIrFactory()
        }

    private fun declarationWithLateBinding(symbol: Symbol, initializer: Element.() -> Unit) = element(Declaration) {
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

    override val rootElement: Element by element(Other, name = "Element") {
        needAcceptMethod()
        needTransformMethod()
        transformByChildren = true
        kDoc = "The root interface of the IR tree. Each IR node implements this interface."

        parent(type(Packages.tree, "BirElementFacade"))

        +field("sourceSpan", type(Packages.tree, "SourceSpan"))
    }
    val statement: Element by element(Other)

    val declaration: Element by element(Declaration) {
        parent(statement)
        parent(symbolOwner)
        parent(mutableAnnotationContainer)

        +field("origin", type(IrPackages.declarations, "IrDeclarationOrigin"))

        generationCallback = {
            println()
            printPropertyDeclaration("parent", declarationParent, VariableKind.VAR)
            println()
        }
    }
    val declarationBase: Element by element(Declaration) {
        //typeKind = TypeKind.Class
        transformByChildren = true
        transformerReturnType = statement
        nameInVisitorMethod = "Declaration"

        parent(declaration)
    }
    val declarationParent: Element by element(Declaration)
    val declarationWithVisibility: Element by element(Declaration) {
        parent(declaration)

        +field("visibility", type(IrPackages.descriptors, "DescriptorVisibility"))
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
        parent(type(Packages.declarations, "BirSymbolOwnerFacade"))

        +declaredSymbol(BirSymbolTree.rootElement)
        +field("signature", idSignatureType, nullable = true, mutable = false)
    }
    val metadataSourceOwner: Element by element(Declaration) {
        kDoc = """
        An [${rootElement.render()}] capable of holding something which backends can use to write
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
        val s = +param("S", BirSymbolTree.rootElement)

        parent(overridableMember)

        +declaredSymbol(s)
        +isFakeOverrideField()
        +referencedSymbolList("overriddenSymbols", s) {
            skipInIrFactory()
        }
    }
    val memberWithContainerSource: Element by element(Declaration) {
        parent(declarationWithName)
    }
    val valueDeclaration: Element by element(Declaration) {
        parent(declarationWithName)
        parent(symbolOwner)

        +declaredSymbol(valueSymbol)
        +field("type", irTypeType)
    }
    val valueParameter: Element by element(Declaration) {
        needTransformMethod()
        kind = ImplementationKind.Interface

        parent(declarationBase)
        parent(valueDeclaration)

        +field("isAssignable", boolean, mutable = false)
        +declaredSymbol(valueParameterSymbol)
        +field("index", int)
        +field("varargElementType", irTypeType, nullable = true)
        +field("isCrossinline", boolean)
        +field("isNoinline", boolean)
        +field("isHidden", boolean) {
            kDoc = """
            If `true`, the value parameter does not participate in [${idSignatureType.render()}] computation.

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

            If a compiler plugin adds parameters to an [${function.render()}],
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
        kind = ImplementationKind.Interface

        parent(declarationBase)
        parent(possiblyExternalDeclaration)
        parent(declarationWithVisibility)
        parent(typeParametersContainer)
        parent(declarationContainer)
        parent(attributeContainer)
        parent(metadataSourceOwner)

        +declaredSymbol(classSymbol)
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
            type<ValueClassRepresentation<*>>().withArgs(irSimpleTypeType),
            nullable = true,
        ) {
            skipInIrFactory()
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
    }
    val mutableAnnotationContainer: Element by element(Declaration) {
        parent(type(Packages.declarations, "BirAnnotationContainer"))

        +listField("annotations", constructorCall, mutability = Var, isChild = false) {
            isOverride = true
            skipInIrFactory()
        }
    }
    val anonymousInitializer: Element by element(Declaration) {
        parent(declarationBase)

        kDoc = """
        Represents a single `init {}` block in a Kotlin class.
        """.trimIndent()

        +declaredSymbol(anonymousInitializerSymbol)
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

        +listField("typeParameters", typeParameter, mutability = MutableList)
    }
    val typeParameter: Element by element(Declaration) {
        needTransformMethod()

        parent(declarationBase)
        parent(declarationWithName)

        +declaredSymbol(typeParameterSymbol)
        +field("variance", type<Variance>())
        +field("index", int)
        +field("isReified", boolean)
        +listField("superTypes", irTypeType, mutability = Var) {
            skipInIrFactory()
        }
    }
    val returnTarget: Element by element(Declaration) {
        parent(symbolOwner)

        +declaredSymbol(returnTargetSymbol)
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

        +declaredSymbol(functionSymbol)
        // NB: there's an inline constructor for Array and each primitive array class.
        +field("isInline", boolean)
        +field("isExpect", boolean)
        +field("returnType", irTypeType) {
            useFieldInIrFactory(customType = irTypeType.copy(nullable = true))
        }
        +field("dispatchReceiverParameter", valueParameter, nullable = true)
        +field("extensionReceiverParameter", valueParameter, nullable = true)
        +listField("valueParameters", valueParameter, mutability = MutableList)
        // The first `contextReceiverParametersCount` value parameters are context receivers.
        +field("contextReceiverParametersCount", int) {
            skipInIrFactory()
        }
        +field("body", body, nullable = true)
    }
    val constructor: Element by element(Declaration) {
        kind = ImplementationKind.Interface

        parent(function)

        +declaredSymbol(constructorSymbol)
        +field("isPrimary", boolean)
    }
    val enumEntry: Element by element(Declaration) {
        kind = ImplementationKind.Interface

        parent(declarationBase)
        parent(declarationWithName)

        +declaredSymbol(enumEntrySymbol)
        +field("initializerExpression", expressionBody, nullable = true)
        +field("correspondingClass", `class`, nullable = true)
    }
    val errorDeclaration: Element by element(Declaration) {
        parent(declarationBase)

        fieldsToSkipInIrFactoryMethod.add("origin")

        +field("symbol", BirSymbolTree.rootElement, mutable = false) {
            skipInIrFactory()
        }
    }
    val functionWithLateBinding: Element by declarationWithLateBinding(simpleFunctionSymbol) {
        parent(simpleFunction)
    }
    val propertyWithLateBinding: Element by declarationWithLateBinding(propertySymbol) {
        parent(property)
    }
    val field: Element by element(Declaration) {
        kind = ImplementationKind.Interface

        parent(declarationBase)
        parent(possiblyExternalDeclaration)
        parent(declarationWithVisibility)
        parent(declarationParent)
        parent(metadataSourceOwner)

        +declaredSymbol(fieldSymbol)
        +field("type", irTypeType)
        +field("isFinal", boolean)
        +field("isStatic", boolean)
        +field("initializer", expressionBody, nullable = true)
        +referencedSymbol("correspondingPropertySymbol", propertySymbol, nullable = true) {
            skipInIrFactory()
        }
    }
    val localDelegatedProperty: Element by element(Declaration) {
        parent(declarationBase)
        parent(declarationWithName)
        parent(symbolOwner)
        parent(metadataSourceOwner)

        +declaredSymbol(localDelegatedPropertySymbol)
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
        +listField("files", file, mutability = MutableList)
    }
    val property: Element by element(Declaration) {
        kind = ImplementationKind.Interface

        parent(declarationBase)
        parent(declarationParent)
        parent(possiblyExternalDeclaration)
        parent(overridableDeclaration.withArgs("S" to propertySymbol))
        parent(metadataSourceOwner)
        parent(attributeContainer)
        parent(memberWithContainerSource)

        +declaredSymbol(propertySymbol)
        +listField("overriddenSymbols", propertySymbol, mutability = Var) {
            skipInIrFactory()
        }
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

        +declaredSymbol(scriptSymbol)
        // NOTE: is the result of the FE conversion, because there script interpreted as a class and has receiver
        // TODO: consider removing from here and handle appropriately in the lowering
        +field("thisReceiver", valueParameter, nullable = true) // K1
        +field("baseClass", irTypeType, nullable = true) // K1
        +listField("explicitCallParameters", variable, mutability = MutableList)
        +listField("implicitReceiversParameters", valueParameter, mutability = MutableList)
        +referencedSymbolList("providedProperties", propertySymbol)
        +listField("providedPropertiesParameters", valueParameter, mutability = MutableList)
        +referencedSymbol("resultProperty", propertySymbol, nullable = true)
        +field("earlierScriptsParameter", valueParameter, nullable = true)
        +referencedSymbolList("importedScripts", scriptSymbol, nullable = true)
        +referencedSymbolList("earlierScripts", scriptSymbol, nullable = true)
        +referencedSymbol("targetClass", classSymbol, nullable = true)
        +field("constructor", constructor, nullable = true, isChild = false) // K1
    }
    val simpleFunction: Element by element(Declaration) {
        kind = ImplementationKind.Interface

        parent(function)
        parent(overridableDeclaration.withArgs("S" to simpleFunctionSymbol))
        parent(attributeContainer)

        +declaredSymbol(simpleFunctionSymbol)
        +listField("overriddenSymbols", simpleFunctionSymbol, mutability = Var) {
            skipInIrFactory()
        }
        +field("isTailrec", boolean)
        +field("isSuspend", boolean)
        +isFakeOverrideField()
        +field("isOperator", boolean)
        +field("isInfix", boolean)
        +referencedSymbol("correspondingPropertySymbol", propertySymbol, nullable = true) {
            skipInIrFactory()
        }
    }
    val typeAlias: Element by element(Declaration) {
        kind = ImplementationKind.Interface

        parent(declarationBase)
        parent(declarationWithName)
        parent(declarationWithVisibility)
        parent(typeParametersContainer)

        +declaredSymbol(typeAliasSymbol)
        +field("isActual", boolean)
        +field("expandedType", irTypeType)
    }
    val variable: Element by element(Declaration) {
        generateIrFactoryMethod = false

        parent(declarationBase)
        parent(valueDeclaration)

        +declaredSymbol(variableSymbol)
        +field("isVar", boolean)
        +field("isConst", boolean)
        +field("isLateinit", boolean)
        +field("initializer", expression, nullable = true)
    }
    val packageFragment: Element by element(Declaration) {
        ownsChildren = false

        parent(declarationContainer)
        parent(symbolOwner)

        +declaredSymbol(packageFragmentSymbol)
        +field("packageFqName", type<FqName>())
    }
    val externalPackageFragment: Element by element(Declaration) {
        transformByChildren = true
        generateIrFactoryMethod = false

        kDoc = """
            This is a root parent element for external declarations (meaning those that come from
            another compilation unit/module, not to be confused with [IrPossiblyExternalDeclaration.isExternal]). 
            
            Each declaration is contained either in some [${file.render()}], or in some [${externalPackageFragment.render()}].
            Declarations coming from dependencies are located in [${externalPackageFragment.render()}].
            
            It can be used for obtaining a module descriptor, which contains the information about
            the module from which the declaration came. It would be more correct to have a link to some
            [${moduleFragment.render()}] instead, which would make [${moduleFragment.render()}] the only source of truth about modules,
            but this is how things are now.
            
            Also, it can be used for checking whether some declaration is external (by checking whether its top
            level parent is an [${externalPackageFragment.render()}]). But it is not possible
            to get all declarations from a fragment. Also, being in the same or different
            fragment doesn't mean anything. There can be more than one fragment for the same dependency.
        """.trimIndent()

        parent(packageFragment)

        +declaredSymbol(externalPackageFragmentSymbol)
        +field("containerSource", type<DeserializedContainerSource>(), nullable = true, mutable = false)
    }
    val file: Element by element(Declaration) {
        needTransformMethod()
        transformByChildren = true
        generateIrFactoryMethod = false

        parent(packageFragment)
        parent(mutableAnnotationContainer)
        parent(metadataSourceOwner)

        +declaredSymbol(fileSymbol)
        +field("fileEntry", type(IrPackages.tree, "IrFileEntry"))
    }

    val expression: Element by element(Expression) {
        needTransformMethod()
        transformByChildren = true

        parent(statement)
        parent(varargElement)
        parent(attributeContainer)

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

        +field("expression", expression) {
            useFieldInIrFactory()
        }
    }
    val blockBody: Element by element(Expression) {
        visitorParameterName = "body"
        generateIrFactoryMethod = true

        parent(body)
        parent(statementContainer)
    }
    val declarationReference: Element by element(Expression) {
        parent(expression)

        +referencedSymbol(BirSymbolTree.rootElement, mutable = false)
        //diff: no accept
    }
    val memberAccessExpression: Element by element(Expression) {
        nameInVisitorMethod = "MemberAccess"
        transformerReturnType = rootElement
        val s = +param("S", BirSymbolTree.rootElement)

        parent(declarationReference)

        +field("dispatchReceiver", expression, nullable = true)
        +field("extensionReceiver", expression, nullable = true)
        +referencedSymbol(s, mutable = false)
        +field("origin", statementOriginType, nullable = true)
        +listField("valueArguments", expression.copy(nullable = true), mutability = MutableList)
        +listField("typeArguments", irTypeType.copy(nullable = true), mutability = Var)
    }
    val functionAccessExpression: Element by element(Expression) {
        nameInVisitorMethod = "FunctionAccess"
        transformerReturnType = rootElement

        parent(memberAccessExpression.withArgs("S" to functionSymbol))

        +field("contextReceiversCount", int)
    }
    val constructorCall: Element by element(Expression) {
        transformerReturnType = rootElement

        parent(functionAccessExpression)
        parent(type<AnnotationMarker>())

        +referencedSymbol(constructorSymbol)
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

        +referencedSymbol(classSymbol)
    }
    val getEnumValue: Element by element(Expression) {

        parent(getSingletonValue)

        +referencedSymbol(enumEntrySymbol)
    }

    val rawFunctionReference: Element by element(Expression) {
        parent(declarationReference)

        kDoc = """
        Represents a platform-specific low-level reference to a function.
        
        On the JS platform it represents a plain reference to a JavaScript function.
        
        On the JVM platform it represents a [java.lang.invoke.MethodHandle] constant.
        """.trimIndent()

        +referencedSymbol(functionSymbol)
    }
    val containerExpression: Element by element(Expression) {
        parent(expression)
        parent(statementContainer)

        +field("origin", statementOriginType, nullable = true)
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

        +declaredSymbol(returnableBlockSymbol)
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

        +field("kind", type(IrPackages.exprs, "IrSyntheticBodyKind"))
    }
    val breakContinue: Element by element(Expression) {
        visitorParameterName = "jump"

        parent(expression)

        +field("loop", loop, isChild = false)
        +field("label", string, nullable = true)
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

        +referencedSymbol(simpleFunctionSymbol)
        +referencedSymbol("superQualifierSymbol", classSymbol, nullable = true)
    }
    val callableReference: Element by element(Expression) {
        val s = +param("S", BirSymbolTree.rootElement)

        parent(memberAccessExpression.withArgs("S" to s))

        +referencedSymbol(s)
    }
    val functionReference: Element by element(Expression) {

        parent(callableReference.withArgs("S" to functionSymbol))

        +field("reflectionTarget", functionSymbol, nullable = true)
    }
    val propertyReference: Element by element(Expression) {
        parent(callableReference.withArgs("S" to propertySymbol))

        +referencedSymbol("field", fieldSymbol, nullable = true)
        +referencedSymbol("getter", simpleFunctionSymbol, nullable = true)
        +referencedSymbol("setter", simpleFunctionSymbol, nullable = true)
    }
    val localDelegatedPropertyReference: Element by element(Expression) {
        parent(callableReference.withArgs("S" to localDelegatedPropertySymbol))

        +referencedSymbol("delegate", variableSymbol)
        +referencedSymbol("getter", simpleFunctionSymbol)
        +referencedSymbol("setter", simpleFunctionSymbol, nullable = true)
    }
    val classReference: Element by element(Expression) {
        parent(declarationReference)

        +referencedSymbol(classifierSymbol)
        +field("classType", irTypeType)
    }
    val const: Element by element(Expression) {
        val t = +param("T")

        parent(expression)

        +field("kind", type(IrPackages.exprs, "IrConstKind").withArgs(t))
        +field("value", t)
    }
    val constantValue: Element by element(Expression) {
        transformByChildren = true
        kind = ImplementationKind.SealedClass

        parent(expression)
    }
    val constantPrimitive: Element by element(Expression) {
        parent(constantValue)

        +field("value", const.withArgs("T" to TypeRef.Star))
    }
    val constantObject: Element by element(Expression) {
        parent(constantValue)

        +referencedSymbol("constructor", constructorSymbol)
        +listField("valueArguments", constantValue, mutability = MutableList)
        +listField("typeArguments", irTypeType, mutability = Var)
    }
    val constantArray: Element by element(Expression) {
        parent(constantValue)

        +listField("elements", constantValue, mutability = MutableList)
    }
    val delegatingConstructorCall: Element by element(Expression) {
        parent(functionAccessExpression)

        +referencedSymbol(constructorSymbol)
    }
    val dynamicExpression: Element by element(Expression) {
        parent(expression)
    }
    val dynamicOperatorExpression: Element by element(Expression) {
        parent(dynamicExpression)

        +field("operator", type(IrPackages.exprs, "IrDynamicOperator"))
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

        +referencedSymbol(constructorSymbol)
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

        +referencedSymbol(fieldSymbol)
        +field("superQualifierSymbol", classSymbol, nullable = true)
        +field("receiver", expression, nullable = true)
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

        +referencedSymbol("classSymbol", classSymbol)
    }
    val loop: Element by element(Expression) {
        visitorParameterName = "loop"
        ownsChildren = false

        parent(expression)

        +field("origin", statementOriginType, nullable = true)
        +field("body", expression, nullable = true)
        +field("condition", expression)
        +field("label", string, nullable = true)
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
        +referencedSymbol("returnTargetSymbol", returnTargetSymbol)
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
        will be represented by [${render()}] with the following list of [arguments]:
        - [${const.render()}] whose `value` is `"i = "`
        - [${getValue.render()}] whose `symbol` will be that of the `i` variable. 
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

        +field("operator", type(IrPackages.exprs, "IrTypeOperator"))
        +field("argument", expression)
        +field("typeOperand", irTypeType)
    }
    val valueAccessExpression: Element by element(Expression) {
        nameInVisitorMethod = "ValueAccess"

        parent(declarationReference)

        +referencedSymbol(valueSymbol)
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
