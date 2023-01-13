/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.ValueClassRepresentation
import org.jetbrains.kotlin.ir.generator.config.AbstractTreeBuilder
import org.jetbrains.kotlin.ir.generator.config.ElementConfig
import org.jetbrains.kotlin.ir.generator.config.ElementConfig.Category.*
import org.jetbrains.kotlin.ir.generator.config.ListFieldConfig.Mutability.List
import org.jetbrains.kotlin.ir.generator.config.ListFieldConfig.Mutability.Var
import org.jetbrains.kotlin.ir.generator.config.SimpleFieldConfig
import org.jetbrains.kotlin.ir.generator.print.toPoet
import org.jetbrains.kotlin.ir.generator.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.types.Variance

// Note the style of the DSL to describe IR elements, which is these things in the following order:
// 1) config (see properties of ElementConfig)
// 2) parents
// 3) fields
object IrTree : AbstractTreeBuilder() {
    private fun symbol(type: TypeRef) = field("symbol", type)
    private fun descriptor(typeName: String) =
        field("descriptor", ClassRef<TypeParameterRef>(TypeKind.Interface, "org.jetbrains.kotlin.descriptors", typeName))

    private val factory: SimpleFieldConfig = field("factory", type(Packages.declarations, "IrFactory"))

    override val rootElement: ElementConfig by element(Other, name = "element") {
        accept = true
        transform = true
        transformByChildren = true

        +field("startOffset", int)
        +field("endOffset", int)
    }
    val statement: ElementConfig by element(Other)

    val declaration: ElementConfig by element(Declaration) {
        parent(statement)
        parent(symbolOwner)
        parent(mutableAnnotationContainerType)

        +descriptor("DeclarationDescriptor")
        +field("origin", type(Packages.declarations, "IrDeclarationOrigin"), mutable = true)
        +field("parent", declarationParent, mutable = true)
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

        +field("visibility", type(Packages.descriptors, "DescriptorVisibility"), mutable = true)
    }
    val declarationWithName: ElementConfig by element(Declaration) {
        parent(declaration)

        +field("name", type<Name>(), mutable = true)
    }
    val possiblyExternalDeclaration: ElementConfig by element(Declaration) {
        parent(declarationWithName)

        +field("isExternal", boolean)
    }
    val symbolOwner: ElementConfig by element(Declaration) {
        +symbol(symbolType)
    }
    val metadataSourceOwner: ElementConfig by element(Declaration) {
        +field("metadata", type(Packages.declarations, "MetadataSource"), nullable = true, mutable = true)
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

        +field("symbol", s)
        +field("isFakeOverride", boolean)
        +listField("overriddenSymbols", s, mutability = Var)
    }
    val memberWithContainerSource: ElementConfig by element(Declaration) {
        parent(declarationWithName)

        +field("containerSource", type<DeserializedContainerSource>(), nullable = true)
    }
    val valueDeclaration: ElementConfig by element(Declaration) {
        parent(declarationWithName)
        parent(symbolOwner)

        +descriptor("ValueDescriptor")
        +symbol(valueSymbolType)
        +field("type", irTypeType, mutable = true)
        +field("isAssignable", boolean)
    }
    val valueParameter: ElementConfig by element(Declaration) {
        transform = true
        visitorParent = declarationBase

        parent(declarationBase)
        parent(valueDeclaration)

        +descriptor("ParameterDescriptor")
        +symbol(valueParameterSymbolType)
        +field("index", int)
        +field("varargElementType", irTypeType, nullable = true, mutable = true)
        +field("isCrossinline", boolean)
        +field("isNoinline", boolean)
        // if true parameter is not included into IdSignature.
        // Skipping hidden params makes IrFunction be look similar to FE.
        // NOTE: it is introduced to fix KT-40980 because more clear solution was not possible to implement.
        // Once we are able to load any top-level declaration from klib this hack should be deprecated and removed.
        +field("isHidden", boolean)
        +field("defaultValue", expressionBody, nullable = true, mutable = true, isChild = true)
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
        +field("modality", type<Modality>(), mutable = true)
        +field("isCompanion", boolean)
        +field("isInner", boolean)
        +field("isData", boolean)
        +field("isValue", boolean)
        +field("isExpect", boolean)
        +field("isFun", boolean)
        +field("source", type<SourceElement>())
        +listField("superTypes", irTypeType, mutability = Var)
        +field("thisReceiver", valueParameter, nullable = true, mutable = true, isChild = true)
        +field(
            "valueClassRepresentation",
            type<ValueClassRepresentation<*>>().withArgs(type(Packages.types, "IrSimpleType")),
            nullable = true,
            mutable = true
        )
        +listField("sealedSubclasses", classSymbolType, mutability = Var)
    }
    val attributeContainer: ElementConfig by element(Declaration) {
        +field("attributeOwnerId", attributeContainer, mutable = true)
    }
    val anonymousInitializer: ElementConfig by element(Declaration) {
        visitorParent = declarationBase

        parent(declarationBase)

        +descriptor("ClassDescriptor") // TODO special descriptor for anonymous initializer blocks
        +symbol(anonymousInitializerSymbolType)
        +field("isStatic", boolean)
        +field("body", blockBody, mutable = true, isChild = true)
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
        +listField("superTypes", irTypeType, mutability = Var)
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
        +field("returnType", irTypeType, mutable = true)
        +field("dispatchReceiverParameter", valueParameter, mutable = true, nullable = true, isChild = true)
        +field("extensionReceiverParameter", valueParameter, mutable = true, nullable = true, isChild = true)
        +listField("valueParameters", valueParameter, mutability = Var, isChild = true)
        // The first `contextReceiverParametersCount` value parameters are context receivers.
        +field("contextReceiverParametersCount", int, mutable = true)
        +field("body", body, mutable = true, nullable = true, isChild = true)
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
        +field("initializerExpression", expressionBody, mutable = true, nullable = true, isChild = true)
        +field("correspondingClass", `class`, mutable = true, nullable = true, isChild = true)
    }
    val errorDeclaration: ElementConfig by element(Declaration) {
        visitorParent = declarationBase

        parent(declarationBase)

        +field("symbol", symbolType) {
            baseGetter = code("error(\"Should never be called\")")
        }
    }
    val functionWithLateBinding: ElementConfig by element(Declaration) {
        typeKind = TypeKind.Interface

        parent(declaration)

        +symbol(simpleFunctionSymbolType)
        +field("modality", type<Modality>(), mutable = true)
        +field("isBound", boolean)
        generationCallback = {
            addFunction(
                FunSpec.builder("acquireSymbol")
                    .addModifiers(KModifier.ABSTRACT)
                    .addParameter("symbol", simpleFunctionSymbolType.toPoet())
                    .returns(simpleFunction.toPoet())
                    .build()
            )
        }
    }
    val propertyWithLateBinding: ElementConfig by element(Declaration) {
        typeKind = TypeKind.Interface

        parent(declaration)

        +symbol(propertySymbolType)
        +field("modality", type<Modality>(), mutable = true)
        +field("getter", simpleFunction, mutable = true, nullable = true)
        +field("setter", simpleFunction, mutable = true, nullable = true)
        +field("isBound", boolean)
        generationCallback = {
            addFunction(
                FunSpec.builder("acquireSymbol")
                    .addModifiers(KModifier.ABSTRACT)
                    .addParameter("symbol", propertySymbolType.toPoet())
                    .returns(property.toPoet())
                    .build()
            )
        }
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
        +field("type", irTypeType, mutable = true)
        +field("isFinal", boolean)
        +field("isStatic", boolean)
        +field("initializer", expressionBody, mutable = true, nullable = true, isChild = true)
        +field("correspondingPropertySymbol", propertySymbolType, mutable = true, nullable = true)
    }
    val localDelegatedProperty: ElementConfig by element(Declaration) {
        visitorParent = declarationBase

        parent(declarationBase)
        parent(declarationWithName)
        parent(symbolOwner)
        parent(metadataSourceOwner)

        +descriptor("VariableDescriptorWithAccessors")
        +symbol(localDelegatedPropertySymbolType)
        +field("type", irTypeType, mutable = true)
        +field("isVar", boolean)
        +field("delegate", variable, mutable = true, isChild = true)
        +field("getter", simpleFunction, mutable = true, isChild = true)
        +field("setter", simpleFunction, mutable = true, nullable = true, isChild = true)
    }
    val moduleFragment: ElementConfig by element(Declaration) {
        visitorParent = rootElement
        transform = true
        transformByChildren = true

        +descriptor("ModuleDescriptor")
        +field("name", type<Name>())
        +field("irBuiltins", type(Packages.tree, "IrBuiltIns"))
        +listField("files", file, mutability = List, isChild = true)
        val undefinedOffset = MemberName(Packages.tree, "UNDEFINED_OFFSET")
        +field("startOffset", int) {
            baseGetter = code("%M", undefinedOffset)
        }
        +field("endOffset", int) {
            baseGetter = code("%M", undefinedOffset)
        }
    }
    val property: ElementConfig by element(Declaration) {
        visitorParent = declarationBase

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
        +field("isExpect", boolean)
        +field("isFakeOverride", boolean)
        +field("backingField", field, mutable = true, nullable = true, isChild = true)
        +field("getter", simpleFunction, mutable = true, nullable = true, isChild = true)
        +field("setter", simpleFunction, mutable = true, nullable = true, isChild = true)
    }

    //TODO: make IrScript as IrPackageFragment, because script is used as a file, not as a class
    //NOTE: declarations and statements stored separately
    val script: ElementConfig by element(Declaration) {
        visitorParent = declarationBase

        parent(declarationBase)
        parent(declarationWithName)
        parent(declarationParent)
        parent(statementContainer)
        parent(metadataSourceOwner)

        +symbol(scriptSymbolType)
        // NOTE: is the result of the FE conversion, because there script interpreted as a class and has receiver
        // TODO: consider removing from here and handle appropriately in the lowering
        +field("thisReceiver", valueParameter, mutable = true, isChild = true, nullable = true) // K1
        +field("baseClass", irTypeType, mutable = true, nullable = true) // K1
        +listField("explicitCallParameters", variable, mutability = Var, isChild = true)
        +listField("implicitReceiversParameters", valueParameter, mutability = Var, isChild = true)
        +listField("providedProperties", propertySymbolType, mutability = Var)
        +listField("providedPropertiesParameters", valueParameter, mutability = Var, isChild = true)
        +field("resultProperty", propertySymbolType, mutable = true, nullable = true)
        +field("earlierScriptsParameter", valueParameter, mutable = true, nullable = true, isChild = true)
        +listField("earlierScripts", scriptSymbolType, mutability = Var, nullable = true)
        +field("targetClass", classSymbolType, mutable = true, nullable = true)
        +field("constructor", constructor, mutable = true, nullable = true) // K1
    }
    val simpleFunction: ElementConfig by element(Declaration) {
        visitorParent = function

        parent(function)
        parent(overridableDeclaration.withArgs("S" to simpleFunctionSymbolType))
        parent(attributeContainer)

        +symbol(simpleFunctionSymbolType)
        +field("isTailrec", boolean)
        +field("isSuspend", boolean)
        +field("isFakeOverride", boolean)
        +field("isOperator", boolean)
        +field("isInfix", boolean)
        +field("correspondingPropertySymbol", propertySymbolType, mutable = true, nullable = true)
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
        +field("expandedType", irTypeType, mutable = true)
    }
    val variable: ElementConfig by element(Declaration) {
        visitorParent = declarationBase

        parent(declarationBase)
        parent(valueDeclaration)

        +descriptor("VariableDescriptor")
        +symbol(variableSymbolType)
        +field("isVar", boolean)
        +field("isConst", boolean)
        +field("isLateinit", boolean)
        +field("initializer", expression, nullable = true, mutable = true, isChild = true)
    }
    val packageFragment: ElementConfig by element(Declaration) {
        visitorParent = rootElement
        ownsChildren = false

        parent(declarationContainer)
        parent(symbolOwner)

        +symbol(packageFragmentSymbolType)
        +field("packageFragmentDescriptor", type(Packages.descriptors, "PackageFragmentDescriptor"))
        +field("fqName", type<FqName>())
    }
    val externalPackageFragment: ElementConfig by element(Declaration) {
        visitorParent = packageFragment
        transformByChildren = true

        parent(packageFragment)

        +symbol(externalPackageFragmentSymbolType)
        +field("containerSource", type<DeserializedContainerSource>(), nullable = true)
    }
    val file: ElementConfig by element(Declaration) {
        transform = true
        transformByChildren = true
        visitorParent = packageFragment

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

        +field("attributeOwnerId", attributeContainer, mutable = true) {
            baseDefaultValue = code("this")
        }
        +field("type", irTypeType, mutable = true)
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

        parent(body)

        +factory
        +field("expression", expression, mutable = true, isChild = true)
    }
    val blockBody: ElementConfig by element(Expression) {
        visitorParent = body
        visitorParam = "body"

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
        suppressPrint = true //todo: generate this element too
        visitorParent = declarationReference
        visitorName = "memberAccess"
        transformerReturnType = rootElement
        val s = +param("S", symbolType)

        parent(declarationReference)

        +field("dispatchReceiver", expression, nullable = true, mutable = true, isChild = true) {
            baseDefaultValue = code("this")
        }
        +field("extensionReceiver", expression, nullable = true, mutable = true, isChild = true) {
            baseDefaultValue = code("this")
        }
        +symbol(s)
        +field("origin", statementOriginType, nullable = true)
        +field("typeArgumentsCount", int)
        +field("typeArgumentsByIndex", type<Array<*>>(irTypeType.copy(nullable = true)))
    }
    val functionAccessExpression: ElementConfig by element(Expression) {
        visitorParent = memberAccessExpression
        visitorName = "functionAccess"
        transformerReturnType = rootElement

        parent(memberAccessExpression.withArgs("S" to functionSymbolType))

        +field("contextReceiversCount", int, mutable = true)
    }
    val constructorCall: ElementConfig by element(Expression) {
        visitorParent = functionAccessExpression
        transformerReturnType = rootElement

        parent(functionAccessExpression)

        +symbol(constructorSymbolType)
        +field("source", type<SourceElement>())
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

        +symbol(classSymbolType)
    }
    val getEnumValue: ElementConfig by element(Expression) {
        visitorParent = getSingletonValue

        parent(getSingletonValue)

        +symbol(enumEntrySymbolType)
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

        +symbol(functionSymbolType)
    }
    val containerExpression: ElementConfig by element(Expression) {
        visitorParent = expression

        parent(expression)
        parent(statementContainer)

        +field("origin", statementOriginType, nullable = true)
        +field("isTransparentScope", boolean)
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

        +field("isTransparentScope", boolean) {
            baseGetter = code("false")
        }
    }
    val composite: ElementConfig by element(Expression) {
        visitorParent = containerExpression

        parent(containerExpression)

        +field("isTransparentScope", boolean) {
            baseGetter = code("true")
        }
    }
    val returnableBlock: ElementConfig by element(Expression) {
        parent(block)
        parent(symbolOwner)
        parent(returnTarget)

        +symbol(returnableBlockSymbolType)
        +field("inlineFunctionSymbol", functionSymbolType, nullable = true)
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

        +field("loop", loop, mutable = true)
        +field("label", string, nullable = true, mutable = true) {
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

        +symbol(simpleFunctionSymbolType)
        +field("superQualifierSymbol", classSymbolType, nullable = true)
    }
    val callableReference: ElementConfig by element(Expression) {
        visitorParent = memberAccessExpression
        val s = +param("S", symbolType)

        parent(memberAccessExpression.withArgs("S" to s))

        +field("referencedName", type<Name>())
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

        +symbol(classifierSymbolType)
        +field("classType", irTypeType, mutable = true)
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

        +field("value", const.withArgs("T" to TypeRef.Star), mutable = true, isChild = true)
    }
    val constantObject: ElementConfig by element(Expression) {
        visitorParent = constantValue

        parent(constantValue)

        +field("constructor", constructorSymbolType)
        +listField("valueArguments", constantValue, mutability = List, isChild = true)
        +listField("typeArguments", irTypeType)
    }
    val constantArray: ElementConfig by element(Expression) {
        visitorParent = constantValue

        parent(constantValue)

        +listField("elements", constantValue, mutability = List, isChild = true)
    }
    val delegatingConstructorCall: ElementConfig by element(Expression) {
        visitorParent = functionAccessExpression

        parent(functionAccessExpression)

        +symbol(constructorSymbolType)
    }
    val dynamicExpression: ElementConfig by element(Expression) {
        visitorParent = expression

        parent(expression)
    }
    val dynamicOperatorExpression: ElementConfig by element(Expression) {
        visitorParent = dynamicExpression

        parent(dynamicExpression)

        +field("operator", type(Packages.exprs, "IrDynamicOperator"))
        +field("receiver", expression, mutable = true, isChild = true)
        +listField("arguments", expression, mutability = List, isChild = true)
    }
    val dynamicMemberExpression: ElementConfig by element(Expression) {
        visitorParent = dynamicExpression

        parent(dynamicExpression)

        +field("memberName", string)
        +field("receiver", expression, mutable = true, isChild = true)
    }
    val enumConstructorCall: ElementConfig by element(Expression) {
        visitorParent = functionAccessExpression

        parent(functionAccessExpression)

        +symbol(constructorSymbolType)
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

        +field("explicitReceiver", expression, nullable = true, mutable = true, isChild = true)
        +listField("arguments", expression, mutability = List, isChild = true)
    }
    val fieldAccessExpression: ElementConfig by element(Expression) {
        visitorParent = declarationReference
        visitorName = "fieldAccess"
        ownsChildren = false

        parent(declarationReference)

        +symbol(fieldSymbolType)
        +field("superQualifierSymbol", classSymbolType, nullable = true)
        +field("receiver", expression, nullable = true, mutable = true, isChild = true) {
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

        +field("value", expression, mutable = true, isChild = true)
    }
    val functionExpression: ElementConfig by element(Expression) {
        visitorParent = expression
        transformerReturnType = rootElement

        parent(expression)

        +field("origin", statementOriginType)
        +field("function", simpleFunction, mutable = true, isChild = true)
    }
    val getClass: ElementConfig by element(Expression) {
        visitorParent = expression

        parent(expression)

        +field("argument", expression, mutable = true, isChild = true)
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
        +field("body", expression, nullable = true, mutable = true, isChild = true) {
            baseDefaultValue = code("null")
        }
        +field("condition", expression, mutable = true, isChild = true)
        +field("label", string, nullable = true, mutable = true) {
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

        +field("value", expression, mutable = true, isChild = true)
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

        +field("suspensionPointIdParameter", variable, mutable = true, isChild = true)
        +field("result", expression, mutable = true, isChild = true)
        +field("resumeResult", expression, mutable = true, isChild = true)
    }
    val suspendableExpression: ElementConfig by element(Expression) {
        visitorParent = expression

        parent(expression)

        +field("suspensionPointId", expression, mutable = true, isChild = true)
        +field("result", expression, mutable = true, isChild = true)
    }
    val `throw`: ElementConfig by element(Expression) {
        visitorParent = expression

        parent(expression)

        +field("value", expression, mutable = true, isChild = true)
    }
    val `try`: ElementConfig by element(Expression) {
        visitorParent = expression
        visitorParam = "aTry"

        parent(expression)

        +field("tryResult", expression, mutable = true, isChild = true)
        +listField("catches", catch, mutability = List, isChild = true)
        +field("finallyExpression", expression, nullable = true, mutable = true, isChild = true)
    }
    val catch: ElementConfig by element(Expression) {
        visitorParent = rootElement
        visitorParam = "aCatch"
        transform = true
        transformByChildren = true

        +field("catchParameter", variable, mutable = true, isChild = true)
        +field("result", expression, mutable = true, isChild = true)
    }
    val typeOperatorCall: ElementConfig by element(Expression) {
        visitorParent = expression
        visitorName = "typeOperator"

        parent(expression)

        +field("operator", type(Packages.exprs, "IrTypeOperator"))
        +field("argument", expression, mutable = true, isChild = true)
        +field("typeOperand", irTypeType, mutable = true)
        +field("typeOperandClassifier", classifierSymbolType)
    }
    val valueAccessExpression: ElementConfig by element(Expression) {
        visitorParent = declarationReference
        visitorName = "valueAccess"

        parent(declarationReference)

        +symbol(valueSymbolType)
        +field("origin", statementOriginType, nullable = true)
    }
    val getValue: ElementConfig by element(Expression) {
        visitorParent = valueAccessExpression

        parent(valueAccessExpression)
    }
    val setValue: ElementConfig by element(Expression) {
        visitorParent = valueAccessExpression

        parent(valueAccessExpression)

        +symbol(valueSymbolType)
        +field("value", expression, mutable = true, isChild = true)
    }
    val varargElement: ElementConfig by element(Expression)
    val vararg: ElementConfig by element(Expression) {
        visitorParent = expression

        parent(expression)

        +field("varargElementType", irTypeType, mutable = true)
        +listField("elements", varargElement, mutability = List, isChild = true)
    }
    val spreadElement: ElementConfig by element(Expression) {
        visitorParent = rootElement
        visitorParam = "spread"
        transform = true
        transformByChildren = true

        parent(varargElement)

        +field("expression", expression, mutable = true, isChild = true)
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

        +field("condition", expression, mutable = true, isChild = true)
        +field("result", expression, mutable = true, isChild = true)
    }
    val elseBranch: ElementConfig by element(Expression) {
        visitorParent = branch
        visitorParam = "branch"
        transform = true
        transformByChildren = true

        parent(branch)
    }
}
