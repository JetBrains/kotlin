/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.ValueClassRepresentation
import org.jetbrains.kotlin.generators.tree.ImplementationKind
import org.jetbrains.kotlin.generators.tree.imports.ArbitraryImportable
import org.jetbrains.kotlin.generators.tree.printer.FunctionParameter
import org.jetbrains.kotlin.generators.tree.printer.VariableKind
import org.jetbrains.kotlin.generators.tree.printer.printFunctionDeclaration
import org.jetbrains.kotlin.generators.tree.printer.printPropertyDeclaration
import org.jetbrains.kotlin.generators.tree.type
import org.jetbrains.kotlin.generators.tree.withArgs
import org.jetbrains.kotlin.ir.generator.IrSymbolTree.anonymousInitializerSymbol
import org.jetbrains.kotlin.ir.generator.IrSymbolTree.classSymbol
import org.jetbrains.kotlin.ir.generator.IrSymbolTree.classifierSymbol
import org.jetbrains.kotlin.ir.generator.IrSymbolTree.constructorSymbol
import org.jetbrains.kotlin.ir.generator.IrSymbolTree.declarationWithAccessorsSymbol
import org.jetbrains.kotlin.ir.generator.IrSymbolTree.enumEntrySymbol
import org.jetbrains.kotlin.ir.generator.IrSymbolTree.externalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.generator.IrSymbolTree.fieldSymbol
import org.jetbrains.kotlin.ir.generator.IrSymbolTree.fileSymbol
import org.jetbrains.kotlin.ir.generator.IrSymbolTree.functionSymbol
import org.jetbrains.kotlin.ir.generator.IrSymbolTree.localDelegatedPropertySymbol
import org.jetbrains.kotlin.ir.generator.IrSymbolTree.packageFragmentSymbol
import org.jetbrains.kotlin.ir.generator.IrSymbolTree.propertySymbol
import org.jetbrains.kotlin.ir.generator.IrSymbolTree.replSnippetSymbol
import org.jetbrains.kotlin.ir.generator.IrSymbolTree.returnTargetSymbol
import org.jetbrains.kotlin.ir.generator.IrSymbolTree.returnableBlockSymbol
import org.jetbrains.kotlin.ir.generator.IrSymbolTree.scriptSymbol
import org.jetbrains.kotlin.ir.generator.IrSymbolTree.simpleFunctionSymbol
import org.jetbrains.kotlin.ir.generator.IrSymbolTree.typeAliasSymbol
import org.jetbrains.kotlin.ir.generator.IrSymbolTree.typeParameterSymbol
import org.jetbrains.kotlin.ir.generator.IrSymbolTree.valueParameterSymbol
import org.jetbrains.kotlin.ir.generator.IrSymbolTree.valueSymbol
import org.jetbrains.kotlin.ir.generator.IrSymbolTree.variableSymbol
import org.jetbrains.kotlin.ir.generator.config.AbstractTreeBuilder
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Element.Category.*
import org.jetbrains.kotlin.ir.generator.model.ListField
import org.jetbrains.kotlin.ir.generator.model.ListField.Mutability.MutableList
import org.jetbrains.kotlin.ir.generator.model.ListField.Mutability.Var
import org.jetbrains.kotlin.ir.generator.model.SimpleField
import org.jetbrains.kotlin.ir.generator.model.symbol.Symbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.AnnotationMarker
import org.jetbrains.kotlin.utils.withIndent

// Note the style of the DSL to describe IR elements, which is these things in the following order:
// 1) config (see properties of Element)
// 2) parents
// 3) fields
object IrTree : AbstractTreeBuilder() {

    private fun descriptor(typeName: String, nullable: Boolean = false): SimpleField =
        field(
            name = "descriptor",
            type = type(Packages.descriptors, typeName),
            mutable = false,
            nullable = nullable,
        ) {
            optInAnnotation = obsoleteDescriptorBasedApiAnnotation
            deepCopyExcludeFromApply = true
        }

    private fun declarationWithLateBinding(symbol: Symbol, initializer: Element.() -> Unit) = element(Declaration) {
        initializer()

        noAcceptMethod()
        noMethodInVisitor()

        +field("isBound", boolean, mutable = false)

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

    private val factory: SimpleField = field("factory", irFactoryType, mutable = false)

    override val rootElement: Element by element(Other, name = "Element") {
        needAcceptMethod()
        needTransformMethod()
        transformByChildren = true

        fun offsetField(prefix: String) = field(prefix + "Offset", int, mutable = true) {
            kDoc = """
            The $prefix offset of the syntax node from which this IR node was generated,
            in number of characters from the start of the source file. If there is no source information for this IR node,
            the [UNDEFINED_OFFSET] constant is used. In order to get the line number and the column number from this offset,
            [IrFileEntry.getLineNumber] and [IrFileEntry.getColumnNumber] can be used.
            
            @see IrFileEntry.getSourceRangeInfo
            """.trimIndent()
            deepCopyExcludeFromApply = true
        }

        +offsetField("start")
        +offsetField("end")

        +field("attributeOwnerId", rootElement, isChild = false) {
            deepCopyExcludeFromApply = true
            kDoc = """
                Original element before copying. Always satisfies the following
                invariant: `this.attributeOwnerId == this.attributeOwnerId.attributeOwnerId`.
            """.trimIndent()
        }

        kDoc = "The root interface of the IR tree. Each IR node implements this interface."
    }
    val statement: Element by element(Other)

    val declaration: Element by element(Declaration) {
        parent(statement)
        parent(symbolOwner)
        parent(mutableAnnotationContainer)

        +descriptor("DeclarationDescriptor")
        +field("origin", type(Packages.declarations, "IrDeclarationOrigin")) {
            deepCopyExcludeFromApply = true
        }
        +factory

        generationCallback = {
            println()
            printPropertyDeclaration("parent", declarationParent, VariableKind.VAR)
            println()
        }
    }
    val declarationBase: Element by element(Declaration) {
        // This class is defined manually, but the entry here needs to be kept actual as well,
        // to correctly generate related code.
        doPrint = false
        kind = ImplementationKind.AbstractClass
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

        +field("isExternal", boolean)
    }
    val symbolOwner: Element by element(Declaration) {
        +declaredSymbol(IrSymbolTree.rootElement)
    }
    val metadataSourceOwner: Element by element(Declaration) {
        val metadataField =
            +field("metadata", type(Packages.declarations, "MetadataSource"), nullable = true) {
                kDoc = """
                The arbitrary metadata associated with this IR node.
                
                @see ${render()}
                """.trimIndent()
                deepCopyExcludeFromApply = true
            }
        kDoc = """
        An [${rootElement.render()}] capable of holding something which backends can use to write
        as the metadata for the declaration.
        
        Technically, it can even be ± an array of bytes, but right now it's usually the frontend representation of the declaration,
        so a descriptor in case of K1, and [org.jetbrains.kotlin.fir.FirElement] in case of K2,
        and the backend invokes a metadata serializer on it to obtain metadata and write it, for example, to `@kotlin.Metadata`
        on JVM.
        
        In Kotlin/Native, [${metadataField.name}] is used to store some LLVM-related stuff in an IR declaration,
        but this is only for performance purposes (before it was done using simple maps).
        """.trimIndent()
    }
    val overridableMember: Element by sealedElement(Declaration) {
        parent(declaration)
        parent(declarationWithVisibility)
        parent(declarationWithName)
        parent(symbolOwner)

        +field("modality", type<Modality>())
    }
    val overridableDeclaration: Element by sealedElement(Declaration) {
        val s = +param("S", IrSymbolTree.rootElement)

        parent(overridableMember)

        +declaredSymbol(s)
        +field("isFakeOverride", boolean)
        +referencedSymbolList("overriddenSymbols", s)
    }
    val memberWithContainerSource: Element by element(Declaration) {
        parent(declarationWithName)

        +field("containerSource", type<DeserializedContainerSource>(), nullable = true, mutable = false)
    }
    val valueDeclaration: Element by element(Declaration) {
        parent(declarationWithName)
        parent(symbolOwner)

        +descriptor("ValueDescriptor")
        +declaredSymbol(valueSymbol)
        +field("type", irTypeType)
    }
    val valueParameter: Element by element(Declaration) {
        doPrint = false
        needTransformMethod()

        parent(declarationBase)
        parent(valueDeclaration)

        +descriptor("ParameterDescriptor")
        +field("isAssignable", boolean)
        +declaredSymbol(valueParameterSymbol)
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

        addImport(ArbitraryImportable(Packages.declarations, "DelicateIrParameterIndexSetter"))
        generationCallback = {
            println()
            printPropertyDeclaration("index", int, VariableKind.VAR, initializer = "-1")
            println()
            withIndent {
                println("@DelicateIrParameterIndexSetter")
                println("set")
            }
        }
    }
    val `class`: Element by element(Declaration) {
        parent(declarationBase)
        parent(possiblyExternalDeclaration)
        parent(declarationWithVisibility)
        parent(typeParametersContainer)
        parent(declarationContainer)
        parent(metadataSourceOwner)

        +descriptor("ClassDescriptor")
        +declaredSymbol(classSymbol)
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
            type<ValueClassRepresentation<*>>().withArgs(type(Packages.types, "IrSimpleType")),
            nullable = true,
        )
        +referencedSymbolList("sealedSubclasses", classSymbol) {
            kDoc = """
            If this is a sealed class or interface, this list contains symbols of all its immediate subclasses.
            Otherwise, this is an empty list.
            
            NOTE: If this [${render()}] was deserialized from a klib, this list will always be empty!
            See [KT-54028](https://youtrack.jetbrains.com/issue/KT-54028).
            """.trimIndent()
        }
    }
    val mutableAnnotationContainer: Element by element(Declaration) {
        parent(type(Packages.declarations, "IrAnnotationContainer"))

        +listField("annotations", constructorCall, mutability = Var, isChild = false) {
            isOverride = true
        }
    }
    val anonymousInitializer: Element by element(Declaration) {
        parent(declarationBase)

        kDoc = """
        Represents a single `init {}` block in a Kotlin class.
        """.trimIndent()

        +descriptor("ClassDescriptor") // TODO special descriptor for anonymous initializer blocks
        +declaredSymbol(anonymousInitializerSymbol)
        +field("isStatic", boolean)
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
        +declaredSymbol(typeParameterSymbol)
        +field("variance", type<Variance>())
        +field("index", int)
        +field("isReified", boolean)
        +listField("superTypes", irTypeType, mutability = Var)
    }
    val returnTarget: Element by element(Declaration) {
        parent(symbolOwner)

        +descriptor("FunctionDescriptor")
        +declaredSymbol(returnTargetSymbol)
    }
    val function: Element by sealedElement(Declaration) {
        doPrint = false

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
        +declaredSymbol(functionSymbol)
        // NB: there's an inline constructor for Array and each primitive array class.
        +field("isInline", boolean)
        +field("isExpect", boolean)
        +field("returnType", irTypeType)
        +field("body", body, nullable = true)
    }
    val constructor: Element by element(Declaration) {
        parent(function)

        +descriptor("ClassConstructorDescriptor")
        +declaredSymbol(constructorSymbol)
        +field("isPrimary", boolean)
    }
    val enumEntry: Element by element(Declaration) {
        parent(declarationBase)
        parent(declarationWithName)

        +descriptor("ClassDescriptor")
        +declaredSymbol(enumEntrySymbol)
        +field("initializerExpression", expressionBody, nullable = true)
        +field("correspondingClass", `class`, nullable = true)
    }
    val functionWithLateBinding: Element by declarationWithLateBinding(simpleFunctionSymbol) {
        parent(simpleFunction)
    }
    val propertyWithLateBinding: Element by declarationWithLateBinding(propertySymbol) {
        parent(property)
    }
    val field: Element by element(Declaration) {
        parent(declarationBase)
        parent(possiblyExternalDeclaration)
        parent(declarationWithVisibility)
        parent(declarationParent)
        parent(metadataSourceOwner)

        +descriptor("PropertyDescriptor")
        +declaredSymbol(fieldSymbol)
        +field("type", irTypeType)
        +field("isFinal", boolean)
        +field("isStatic", boolean)
        +field("initializer", expressionBody, nullable = true)
        +referencedSymbol("correspondingPropertySymbol", propertySymbol, nullable = true)
    }
    val localDelegatedProperty: Element by element(Declaration) {
        parent(declarationBase)
        parent(declarationWithName)
        parent(symbolOwner)
        parent(metadataSourceOwner)

        +descriptor("VariableDescriptorWithAccessors")
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

        +descriptor("ModuleDescriptor").apply {
            optInAnnotation = null
        }
        +field("name", type<Name>(), mutable = false)
        +listField("files", file, mutability = MutableList)

        generationCallback = {
            printlnMultiLine(
                """
 
                @Deprecated("", level = DeprecationLevel.HIDDEN) // See KT-75353
                fun <D> transform(
                    transformer: @Suppress("DEPRECATION_ERROR") org.jetbrains.kotlin.ir.visitors.IrElementTransformer<D>,
                    data: D
                ): IrModuleFragment = transform(transformer as IrTransformer<D>, data)
                """
            )
        }
    }
    val property: Element by element(Declaration) {
        parent(declarationBase)
        parent(possiblyExternalDeclaration)
        parent(overridableDeclaration.withArgs("S" to propertySymbol))
        parent(metadataSourceOwner)
        parent(memberWithContainerSource)

        +descriptor("PropertyDescriptor")
        +declaredSymbol(propertySymbol)
        +listField("overriddenSymbols", propertySymbol, mutability = Var)
        +field("isVar", boolean)
        +field("isConst", boolean)
        +field("isLateinit", boolean)
        +field("isDelegated", boolean)
        +field("isExpect", boolean)
        +field("backingField", field, nullable = true)
        +field("getter", simpleFunction, nullable = true)
        +field("setter", simpleFunction, nullable = true)
    }

    //TODO: make IrScript as IrPackageFragment, because script is used as a file, not as a class
    //NOTE: declarations and statements stored separately
    val script: Element by element(Declaration) {
        parent(declarationBase)
        parent(declarationWithName)
        parent(declarationParent)
        parent(statementContainer)
        parent(metadataSourceOwner)

        +declaredSymbol(scriptSymbol)
        +descriptor("ScriptDescriptor")
        // NOTE: is the result of the FE conversion, because there script interpreted as a class and has receiver
        // TODO: consider removing from here and handle appropriately in the lowering
        +field("thisReceiver", valueParameter, nullable = true) // K1
        +field("baseClass", irTypeType, nullable = true) {
            deepCopyExcludeFromApply = true
        } // K1
        +listField("explicitCallParameters", variable, mutability = Var)
        +listField("implicitReceiversParameters", valueParameter, mutability = Var)
        +referencedSymbolList("providedProperties", propertySymbol) {
            deepCopyExcludeFromApply = true
        }
        +listField("providedPropertiesParameters", valueParameter, mutability = Var)
        +referencedSymbol("resultProperty", propertySymbol, nullable = true)
        +field("earlierScriptsParameter", valueParameter, nullable = true)
        +referencedSymbolList("importedScripts", scriptSymbol, nullable = true)
        +referencedSymbolList("earlierScripts", scriptSymbol, nullable = true)
        +referencedSymbol("targetClass", classSymbol, nullable = true)
        +field("constructor", constructor, nullable = true, isChild = false) {
            deepCopyExcludeFromApply = true
        } // K1
    }
    val replSnippet: Element by element(Declaration) {
        parent(declarationBase)
        parent(declarationWithName)
        parent(declarationParent)
        parent(metadataSourceOwner)

        kDoc = """
            Represents a REPL snippet entity that corresponds to the analogous FIR entity.
        """.trimIndent()

        +declaredSymbol(replSnippetSymbol)
        +listField("receiverParameters", valueParameter, mutability = Var) {
            kDoc = """
                Stores implicit receiver parameters configured for the snippet.
            """.trimIndent()
        }
        +listField("variablesFromOtherSnippets", variable, mutability = MutableList)
        +listField("declarationsFromOtherSnippets", declaration, mutability = MutableList)
        +referencedSymbol("stateObject", classSymbol, nullable = true) {
            kDoc = """
                Contains link to the static state object for this compilation session.
            """.trimIndent()
        }
        +field("body", body)
        +field("returnType", irTypeType, nullable = true)
        +referencedSymbol("targetClass", classSymbol, nullable = true){
            kDoc = """
                Contains link to the IrClass symbol to which this snippet should be lowered on the appropriate stage.
            """.trimIndent()
        }
    }
    val simpleFunction: Element by element(Declaration) {
        parent(function)
        parent(overridableDeclaration.withArgs("S" to simpleFunctionSymbol))

        +descriptor("FunctionDescriptor")
        +declaredSymbol(simpleFunctionSymbol)
        +listField("overriddenSymbols", simpleFunctionSymbol, mutability = Var)
        +field("isTailrec", boolean)
        +field("isSuspend", boolean)
        +field("isOperator", boolean)
        +field("isInfix", boolean)
        +referencedSymbol("correspondingPropertySymbol", propertySymbol, nullable = true)
    }
    val typeAlias: Element by element(Declaration) {
        parent(declarationBase)
        parent(declarationWithName)
        parent(declarationWithVisibility)
        parent(typeParametersContainer)
        parent(metadataSourceOwner)

        +descriptor("TypeAliasDescriptor")
        +declaredSymbol(typeAliasSymbol)
        +field("isActual", boolean)
        +field("expandedType", irTypeType)
    }
    val variable: Element by element(Declaration) {
        parent(declarationBase)
        parent(valueDeclaration)

        +descriptor("VariableDescriptor")
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
    }
    val file: Element by element(Declaration) {
        needTransformMethod()
        transformByChildren = true

        parent(packageFragment)
        parent(mutableAnnotationContainer)
        parent(metadataSourceOwner)

        +declaredSymbol(fileSymbol)
        +field("module", moduleFragment, isChild = false) {
            deepCopyExcludeFromApply = true
        }
        +field("fileEntry", type(Packages.tree, "IrFileEntry"))
    }

    val expression: Element by element(Expression) {
        needTransformMethod()
        transformByChildren = true

        parent(statement)
        parent(varargElement)

        +field("type", irTypeType)
    }
    val statementContainer: Element by element(Expression) {
        ownsChildren = false

        +listField("statements", statement, mutability = MutableList)
    }
    val body: Element by sealedElement(Expression) {
        needTransformMethod()
        visitorParameterName = "body"
        transformByChildren = true
        kind = ImplementationKind.AbstractClass
    }
    val expressionBody: Element by element(Expression) {
        needTransformMethod()
        visitorParameterName = "body"

        parent(body)

        +field("expression", expression)
    }
    val blockBody: Element by element(Expression) {
        visitorParameterName = "body"

        parent(body)
        parent(statementContainer)
    }
    val declarationReference: Element by element(Expression) {
        parent(expression)

        +referencedSymbol(IrSymbolTree.rootElement, mutable = false)
        //diff: no accept
    }
    val memberAccessExpression: Element by element(Expression) {
        doPrint = false
        nameInVisitorMethod = "MemberAccess"
        transformerReturnType = rootElement
        val s = +param("S", IrSymbolTree.rootElement)

        parent(declarationReference)

        +referencedSymbol(s, mutable = false)
        +field("origin", statementOriginType, nullable = true)
        +listField(
            name = "typeArguments",
            baseType = irTypeType.copy(nullable = true),
            mutability = MutableList,
        ) {
            deepCopyExcludeFromConstructor = true
            deepCopyExcludeFromApply = true
        }
    }
    val functionAccessExpression: Element by sealedElement(Expression) {
        nameInVisitorMethod = "FunctionAccess"
        transformerReturnType = rootElement

        parent(memberAccessExpression.withArgs("S" to functionSymbol))
    }
    val constructorCall: Element by element(Expression) {
        transformerReturnType = rootElement

        parent(functionAccessExpression)
        parent(type<AnnotationMarker>())

        +referencedSymbol(constructorSymbol)
        +field("source", type<SourceElement>())
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

        +field("inlinedFunctionStartOffset", int) {
            kDoc = """
                Represents the start offset of the inlined function that was located inside `fileEntry`.
            """.trimIndent()
        }
        +field("inlinedFunctionEndOffset", int) {
            kDoc = """
                Represents the end offset of the inlined function that was located inside `fileEntry`.                
            """.trimIndent()
        }
        +field("inlinedFunctionSymbol", functionSymbol, isChild = false, nullable = true)
        +field("inlinedFunctionFileEntry", type(Packages.tree, "IrFileEntry"), isChild = false)
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
        val s = +param("S", IrSymbolTree.rootElement)

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

    // TODO: extract common part of function/property reference to common supertype - KT-73206
    val richFunctionReference: Element by element(Expression) {
        parent(expression)

        +referencedSymbol("reflectionTargetSymbol", functionSymbol, nullable = true)
        +referencedSymbol("overriddenFunctionSymbol", simpleFunctionSymbol, nullable)
        +listField("boundValues", expression, nullable = false, mutability = ListField.Mutability.MutableList)
        +field("invokeFunction", simpleFunction)
        +field("origin", statementOriginType, nullable = true)
        +field("hasUnitConversion", boolean)
        +field("hasSuspendConversion", boolean)
        +field("hasVarargConversion", boolean)
        +field("isRestrictedSuspension", boolean)

        kDoc = """
            This node is intended to unify different ways of handling function reference-like objects in IR.

            In particular, it covers:
            * Lambdas and anonymous functions
            * Regular function references (`::foo`, and `receiver::foo` in code)
            * Adapted function references, which happen in cases where referenced function doesn't perfectly match the expected shape, such as:
               * Returns something instead of expected `Unit`
               * Declares more parameters than expected, but those extra parameters have default values
               * Consumes vararg instead of an expected fixed number of arguments
               * Is not suspend, while suspend function is expected
               * Is a reference to a `fun interface` / SAM interface constructor, which is not a real function at all
            * SAM or `fun interface` conversions of something listed above. E.g. `Callable { 123 }` or `Callable(::foo)`

            This node is intended to replace [IrFunctionReference] and [IrFunctionExpression] in the IR tree.
            It also replaces some adapted function references implemented as [IrBlock] with [IrFunction] and [IrFunctionReference] inside it.

            Such objects are eventually transformed to anonymous classes, which implement the corresponding interface.
            For example:
            
            ```
            fun String.test(x: Int) : Unit = TODO()
            
            fun interface Foo {
               fun bar(x: String, y: Int): Unit
            }
            
            fun main() {
                val x = "OK"::test
                //
                // val x = { // BLOCK
                //   class <anonymous>(val boundValue: String) : KFunction1<Int, Unit> {
                //     override fun invoke(p0: Int) { invokeFunction(boundValue, p0) }
                //     private static fun invokeFunction(p0: String, p1: Int) = p0.test(p1)
                //     // reflection information
                //   }
                //   <anonymous>("OK")
                // }
            
                val y = Foo(String::test)
                // val y = { // BLOCK
                //   class <anonymous>() : Foo {
                //     override fun bar(x: String, y: Int) { invokeFunction(x, y) }
                //     private static fun invokeFunction(p0: String, p1: Int) = p0.test(p1)
                //     // reflection information
                //   }
                //   <anonymous>()
                // }
            }
            ```
            
            In general case, the mental model of this node is the following instance of local anonymous class:
            ```
            class <anonymous>(
                private val boundValue0: %boundValue0Type%,
                private val boundValue1: %boundValue1Type%,
                ...
            ): %ExpressionType% {
                // moved from [invokeFunction] property
                // may be inlined to overriddenFunctionName as optimization
                private static fun invokeFunction(...) : %ReturnType% {
                   // some way of invoke [reflectionTargetSymbol] or body of original lambda
                   // it can be transformed by lowerings and plugins as other function bodies,
                   // so no assumptions should be made on specific content of it
                }
            
                // overriding function [overriddenFunctionSymbol]
                // would be created later, when node would be transformed to a class
                // it can't be referenced explicitly, all calls would happen with function from super-interface
                override fun %overriddenFunctionName%(
                    overriddenFunctionParameter0: %overriddenFunctionParametersType0%,
                    overriddenFunctionParameter1: %overriddenFunctionParametersType1%,
                    ...
                ) = invokeFunction(
                    boundValue0, boundValue1, ..., boundValueN,
                    overriddenFunctionParameter0, overriddenFunctionParameter1, ..., overriddenFunctionParameterN
                )
            
                // if reflectionTarget is not null
                //    some platform-specific implementation of reflection information for reflectionTarget
                //    some platform-specific implementation of equality/hashCode based on reflectionTarget
            }
            val theReference = <anonymous>(boundValues[0], boundValues[1], ..., boundValues[N])
            ```
            
            So basically, this is an anonymous object implementing expression type, capturing `boundValues`, and overriding the function stored in
            [overriddenFunctionSymbol] by the function stored in [invokeFunction], with reflection information for [reflectionTargetSymbol]
            if it is not null.
            
            [invokeFunction] parameters except first [boundValues.size] correspond to non-dispatch parameters of [overriddenFunctionSymbol]
            in natural order (i.e., contexts, extension, regular). The mapping between [invokeFunction] and [reflectionTargetSymbol] parameters
            is not specified, and shouldn't be used. Instead, a body inside [invokeFunction] should be processed as regular expressions.
            [boundValues] would be computed on reference creation, and then loaded from the reference object on invocation.
            
            [overriddenFunctionSymbol] is typically the corresponding `invoke` method of the `(K)(Suspend)FunctionN` interface, but it also can be
            the method of a fun interface or Java SAM interface, if the corresponding SAM conversion has happened.
            
            [reflectionTargetSymbol] is typically a function for which the reference was initially created, or null if it is a lambda, which doesn't
            need any reflection information.
            
            [hasUnitConversion], [hasSuspendConversion], [hasVarargConversion], [isRestrictedSuspension] flags represent information about
            the reference, which is useful for generating correct reflection information. While it's technically possible to reconstruct it from
            the function and reflection function signature, it's easier and more robust to store it explicitly.
            
            This allows processing function references by almost all lowerings as normal calls (within [invokeFunction]), and minimizes special
            cases. Also, it enables support of several bound values.
        """.trimIndent()
    }
    val richPropertyReference: Element by element(Expression) {
        parent(expression)

        +referencedSymbol("reflectionTargetSymbol", declarationWithAccessorsSymbol, nullable = true)
        +listField("boundValues", expression, nullable = false, mutability = ListField.Mutability.MutableList)
        +field("getterFunction", simpleFunction)
        +field("setterFunction", simpleFunction, nullable = true)
        +field("origin", statementOriginType, nullable = true)

        kDoc = """
            This node is intended to unify different ways of handling property reference-like objects in IR.

            In particular, it covers:
              * References to regular properties
              * References implicitly passed to property delegation functions
              * References implicitly passed to local variable delegation functions (see [IrLocalDelegatedProperty])

            This node is intended to replace [IrPropertyReference] and [IrLocalDelegatedPropertyReference] in the IR tree.

            It's similar to [IrRichFunctionReference] except for property references, and has the same semantics, with the following differences:
              * There is no [IrRichFunctionReference.overriddenFunctionSymbol] because property references cannot implement a `fun interface`
                or be SAM-converted
              * There is no [IrRichFunctionReference.invokeFunction], but there is [getterFunction] with similar semantics instead
              * There is nullable [setterFunction] with similar semantics in case of mutable property
              * [boundValues] are passed as the first arguments to both [getterFunction] and [setterFunction]
        """.trimIndent()
    }

    val classReference: Element by element(Expression) {
        parent(declarationReference)

        +referencedSymbol(classifierSymbol)
        +field("classType", irTypeType)
    }
    val const: Element by element(Expression) {
        parent(expression)

        +field("kind", type(Packages.exprs, "IrConstKind"))
        +field("value", anyType, nullable = true)
    }
    val constantValue: Element by element(Expression) {
        transformByChildren = true
        kind = ImplementationKind.SealedClass

        parent(expression)
    }
    val constantPrimitive: Element by element(Expression) {
        parent(constantValue)

        +field("value", const)
    }
    val constantObject: Element by element(Expression) {
        parent(constantValue)

        +referencedSymbol("constructor", constructorSymbol)
        +listField("valueArguments", constantValue, mutability = MutableList)
        +listField("typeArguments", irTypeType, mutability = MutableList)
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
        +field("origin", statementOriginType, nullable = true)
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
