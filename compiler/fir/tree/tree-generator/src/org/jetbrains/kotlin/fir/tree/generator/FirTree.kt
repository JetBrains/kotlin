/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.fir.tree.generator.FirTree.FieldSets.annotations
import org.jetbrains.kotlin.fir.tree.generator.FirTree.FieldSets.declarations
import org.jetbrains.kotlin.fir.tree.generator.FirTree.FieldSets.typeArguments
import org.jetbrains.kotlin.fir.tree.generator.FirTree.FieldSets.typeParameters
import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.fir.tree.generator.model.Element.Kind.*
import org.jetbrains.kotlin.fir.tree.generator.model.fieldSet
import org.jetbrains.kotlin.fir.tree.generator.util.type
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.fir.tree.generator.model.Element.Kind.TypeRef as TypeRefElement

// Note the style of the DSL to describe FIR elements, which is these things in the following order:
// 1) config (see properties of Element)
// 2) parents
// 3) fields
object FirTree : AbstractFirTreeBuilder() {
    override val rootElement: Element by element(Other, name = "Element") {
        hasAcceptChildrenMethod = true
        hasTransformChildrenMethod = true

        +field("source", sourceElementType, nullable = true)
    }

    val annotationContainer: Element by element(Other) {
        +annotations
    }

    val typeParameterRef: Element by element(Declaration) {
        +referencedSymbol(typeParameterSymbolType)
    }

    val typeParametersOwner: Element by sealedElement(Declaration) {
        parent(typeParameterRefsOwner)

        +typeParameters {
            withTransform = true
        }
    }

    val typeParameterRefsOwner: Element by sealedElement(Declaration) {
        +listField("typeParameters", typeParameterRef, withTransform = true)
    }

    val resolvable: Element by sealedElement(Expression) {
        +field("calleeReference", reference, withReplace = true, withTransform = true)
    }

    val diagnosticHolder: Element by element(Diagnostics) {
        +field("diagnostic", coneDiagnosticType)
    }

    val controlFlowGraphOwner: Element by element(Declaration) {
        +field("controlFlowGraphReference", controlFlowGraphReference, withReplace = true, nullable = true)
    }

    val elementWithResolveState: Element by element(Other) {
        kind = ImplementationKind.AbstractClass

        +field("resolvePhase", resolvePhaseType) { isParameter = true; }
        +field("resolveState", resolveStateType) {
            isMutable = true; isVolatile = true; isFinal = true
            implementationDefaultStrategy = AbstractField.ImplementationDefaultStrategy.Lateinit
            customInitializationCall = "resolvePhase.asResolveState()"
            arbitraryImportables += phaseAsResolveStateExtentionImport
            optInAnnotation = resolveStateAccessAnnotation
        }
        +field("moduleData", firModuleDataType)
    }

    val declaration: Element by sealedElement(Declaration) {
        kind = ImplementationKind.AbstractClass

        parent(elementWithResolveState)
        parent(annotationContainer)

        +declaredSymbol(firBasedSymbolType.withArgs(declaration))
        +field("moduleData", firModuleDataType)
        +field("origin", declarationOriginType)
        +field("attributes", declarationAttributesType)
    }

    val callableDeclaration: Element by sealedElement(Declaration) {
        kDoc = """
            Represents a common base for all callable declarations in FIR.
            This includes all function-like declarations (see [FirFunction] and all variable-like declarations (see [FirVariable]).

            Notable properties:
            - [symbol] — the symbol which serves as a pointer to this càllable declaration.
            - [typeParameters] — type parameter references declared for this callable declaration, if any.
            In certain situations, references to type parameters of its outer classes may also be present in the list. 
            - [dispatchReceiverType] — dispatch receiver type for non-static member callables, or null for top-level or static callables.
            Dispatch receiver type is a type of `this` based on the member callable's owner class and used to determine accessible scopes.
            - [contextParameters] — context parameters of the callable declaration, if any.
            - [receiverParameter] — the extension receiver parameter if present, otherwise null.
            - [returnTypeRef] — the declared return type of the function-like declaration, or the type of the variable-like declaration.
            - [annotations] — annotations present on the declaration, if any.
            - [isLocal] — the callable is non-local (isLocal = false) iff all its ancestors (containing declarations) are
            either files (see [FirFile]) or classes.  A property accessor or a backing field inherits isLocal from its owner property, 
            otherwise with any callable or anonymous initializer among ancestors, the declaration is local (isLocal = true).
            In particular, it means that any callable member of a local class is also local. 
                """.trimIndent()
        parent(memberDeclaration)

        +field("returnTypeRef", typeRef, withReplace = true, withTransform = true)
        +field("receiverParameter", receiverParameter, nullable = true, withReplace = true, withTransform = true)
        +field("deprecationsProvider", deprecationsProviderType, withReplace = true) {
            isMutable = true
        }
        +referencedSymbol(callableSymbolType.withArgs(callableDeclaration))
        +field("containerSource", type<DeserializedContainerSource>(), nullable = true)
        +field("dispatchReceiverType", coneSimpleKotlinTypeType, nullable = true)
        +listField(name = "contextParameters", valueParameter, useMutableOrEmpty = true, withReplace = true, withTransform = true)
    }

    val function: Element by sealedElement(Declaration) {
        kDoc = """
            Represents a common base for Kotlin function-like declarations in FIR (all kinds of functions, property accessors, and constructors).
            
            This element is inherited by [FirNamedFunction], [FirAnonymousFunction], [FirPropertyAccessor], and [FirConstructor].
            
            Notable properties common to functions:
            - [symbol] — the symbol which serves as a pointer to this function-like declaration.
            - [valueParameters] — the list of value parameters.
            - [dispatchReceiverType] — dispatch receiver type for member functions, or null for top-level or static functions.
            Dispatch receiver type is a type of `this` based on the member function's owner class and used to determine accessible scopes.
            - [contextParameters] — context parameters of the function, if any.
            - [receiverParameter] — the extension receiver parameter if present, otherwise null.
            - [returnTypeRef] — the declared return type of the function-like declaration.
            - [body] — the function body, if present, otherwise null.
            - [annotations] — annotations present on the declaration, if any.
            - [isLocal] — the function is non-local (isLocal = false) iff all its ancestors (containing declarations) are
            either files (see [FirFile]) or classes. A property accessor inherits isLocal from its owner property, 
            otherwise with any callable or anonymous initializer among ancestors, the declaration is local (isLocal = true).
            In particular, it means that any member function of a local class is also local. 
                """.trimIndent()
        parent(callableDeclaration)
        parent(targetElement)
        parent(controlFlowGraphOwner)
        parent(statement)

        +declaredSymbol(functionSymbolType.withArgs(function))
        +listField(valueParameter, withReplace = true, withTransform = true)
        +field("body", block, nullable = true, withReplace = true, withTransform = true)
    }

    val errorExpression: Element by element(Expression) {
        parent(expression)
        parent(diagnosticHolder)

        +field("expression", expression, nullable = true)
        +field("nonExpressionElement", rootElement, nullable = true)
    }

    val errorFunction: Element by element(Declaration) {
        parent(function)
        parent(diagnosticHolder)

        +declaredSymbol(errorFunctionSymbolType)
    }

    val memberDeclaration: Element by sealedElement(Declaration) {
        kDoc = """
            Represents a common base for all declarations in FIR, that can be class member declarations (though in fact can be
            declared e.g. at top-level). This includes all class-like declarations (see [FirClassLikeDeclaration] and
            all callable declarations (see [FirCallableDeclaration]).

            Notable properties:
            - [symbol] — the symbol which serves as a pointer to this declaration.
            - [typeParameters] — type parameter references declared for this declaration, if any.
            In certain situations, references to type parameters of its outer classes may also be present in the list. 
            - [isLocal] — the declaration is non-local (isLocal = false) iff all its ancestors (containing declarations) are
            either files (see [FirFile]) or classes. A property accessor or a backing field inherits isLocal from its owner property, 
            otherwise with any callable or anonymous initializer among ancestors, the declaration is local (isLocal = true).
            In particular, it means that any member declaration of a local class is also local. 
                """.trimIndent()
        parent(declaration)
        parent(typeParameterRefsOwner)

        +field("status", declarationStatus, withReplace = true, withTransform = true)
        +field("isLocal", boolean)
    }

    val statement: Element by element(Expression) {
        parent(annotationContainer)
    }

    val expression: Element by element(Expression) {
        parent(statement)

        +field("coneTypeOrNull", coneKotlinTypeType, nullable = true, withReplace = true) {
            optInAnnotation = unresolvedExpressionTypeAccessAnnotation
        }
        +annotations
    }

    val lazyExpression: Element by element(Expression) {
        parent(expression)
    }

    val argumentList: Element by element(Expression) {
        +listField("arguments", expression, withTransform = true)
    }

    // TODO: may smth like `CallWithArguments` or `ElementWithArguments`?
    val call: Element by sealedElement(Expression) {
        parent(statement)

        +field(argumentList, withReplace = true)
    }

    val block: Element by element(Expression) {
        needTransformOtherChildren()

        parent(expression)

        +listField(statement, withTransform = true)
        +field("isUnitCoerced", boolean, withReplace = true)
    }

    val lazyBlock: Element by element(Expression) {
        parent(block)
    }

    val booleanOperatorExpression: Element by element(Expression) {
        needTransformOtherChildren()

        parent(expression)

        +field("leftOperand", expression, withTransform = true)
        +field("rightOperand", expression, withTransform = true)
        +field("kind", operationKindType)
    }

    val targetElement: Element by element(Other)

    val jump: Element by sealedElement(Expression) {
        val e = +param("E", targetElement)

        parent(expression)

        +field("target", jumpTargetType.withArgs(e))
    }

    val loopJump: Element by element(Expression) {
        parent(jump.withArgs("E" to loop))
    }

    val breakExpression: Element by element(Expression) {
        parent(loopJump)
    }

    val continueExpression: Element by element(Expression) {
        parent(loopJump)
    }

    val returnExpression: Element by element(Expression) {
        needTransformOtherChildren()

        parent(jump.withArgs("E" to function))

        +field("result", expression, withTransform = true)
    }

    val label: Element by element(Other) {
        +field("name", string)
    }

    val loop: Element by sealedElement(Expression) {

        parent(statement)
        parent(targetElement)

        +field(block, withTransform = true)
        +field("condition", expression, withTransform = true)
        +field(label, nullable = true, withTransform = true)
    }

    val whileLoop: Element by element(Expression) {
        parent(loop)

        +field("condition", expression, withTransform = true)
        +field(block, withTransform = true)
    }

    val doWhileLoop: Element by element(Expression) {
        parent(loop)
    }

    val errorLoop: Element by element(Expression) {
        parent(loop)
        parent(diagnosticHolder)
    }

    val catchClause: Element by element(Expression, name = "Catch") {
        needTransformOtherChildren()

        +field("parameter", property, withTransform = true)
        +field(block, withTransform = true)
    }

    val tryExpression: Element by element(Expression) {
        needTransformOtherChildren()

        parent(expression)
        parent(resolvable)

        +field("tryBlock", block, withTransform = true)
        +listField("catches", catchClause, withTransform = true)
        +field("finallyBlock", block, nullable = true, withTransform = true)
    }

    val elvisExpression: Element by element(Expression) {
        parent(expression)
        parent(resolvable)

        +field("lhs", expression, withTransform = true)
        +field("rhs", expression, withTransform = true)
    }

    val contextArgumentListOwner: Element by element(Expression) {
        +listField("contextArguments", expression, useMutableOrEmpty = true, withReplace = true, withTransform = true)
    }

    val qualifiedAccessExpression: Element by element(Expression) {
        parent(expression)
        parent(resolvable)
        parent(contextArgumentListOwner)

        +typeArguments {
            withTransform = true
        }
        +field("explicitReceiver", expression, nullable = true, withReplace = true, withTransform = true)
        +field("dispatchReceiver", expression, nullable = true, withReplace = true)
        +field("extensionReceiver", expression, nullable = true, withReplace = true)
        +field("source", sourceElementType, nullable = true, withReplace = true)
        +listField("nonFatalDiagnostics", coneDiagnosticType, useMutableOrEmpty = true, withReplace = true)
    }

    val qualifiedErrorAccessExpression: Element by element(Expression) {
        parent(expression)
        parent(diagnosticHolder)

        +field("selector", errorExpression, withTransform = true)
        +field("receiver", expression, withReplace = true)
    }

    val literalExpression: Element by element(Expression) {
        parent(expression)

        +field("kind", constKindType, withReplace = true)
        +field("value", anyType, nullable = true)
        +field("prefix", string, nullable = true)
    }

    val functionCall: Element by element(Expression) {
        parent(qualifiedAccessExpression)
        parent(call)

        +field("calleeReference", namedReference)
        +field("origin", functionCallOrigin)
    }

    val integerLiteralOperatorCall: Element by element(Expression) {
        parent(functionCall)

        // we need methods for transformation of receivers
        +field("dispatchReceiver", expression, nullable = true, withReplace = true, withTransform = true)
        +field("extensionReceiver", expression, nullable = true, withReplace = true, withTransform = true)
    }

    val collectionLiteral: Element by element(Expression) {
        parent(expression)
        parent(call)
    }

    val checkNotNullCall: Element by element(Expression) {
        parent(expression)
        parent(call)
        parent(resolvable)
    }

    val comparisonExpression: Element by element(Expression) {
        parent(expression)

        +field("operation", operationType)
        +field("compareToCall", functionCall)
    }

    val typeOperatorCall: Element by element(Expression) {
        needTransformOtherChildren()

        parent(expression)
        parent(call)

        +field("operation", operationType)
        +field("conversionTypeRef", typeRef, withTransform = true, withReplace = true)
        +field("argFromStubType", boolean, withReplace = true)
    }

    val augmentedAssignment: Element by element(Expression) {
        kDoc = """
                Represents an augmented assignment statement (e.g. `x += y`) **before** it gets resolved.
                After resolution, it will be either represented as an assignment (`x = x.plus(y)`) or a call (`x.plusAssign(y)`). 
                
                Augmented assignments with an indexed access as receiver are represented as [${indexedAccessAugmentedAssignment.render()}]. 
            """.trimIndent()

        parent(statement)

        +field("operation", operationType)
        +field("leftArgument", expression, withTransform = true)
        +field("rightArgument", expression, withTransform = true)
    }

    val incrementDecrementExpression: Element by element(Expression) {
        parent(expression)

        +field("isPrefix", boolean)
        +field("operationName", nameType)
        +field("expression", expression, withReplace = true)
        +field("operationSource", sourceElementType, nullable = true)
    }

    val equalityOperatorCall: Element by element(Expression) {
        parent(expression)
        parent(call)

        +field("operation", operationType)
    }

    val whenBranch: Element by element(Expression) {
        needTransformOtherChildren()

        +field("condition", expression, withTransform = true)
        +field("result", block, withTransform = true)
        +field("hasGuard", boolean)
    }

    val classLikeDeclaration: Element by sealedElement(Declaration) {
        kDoc = """
            Represents a common base for all class-like declarations in FIR.
            This includes named and anonymous classes (see [FirClass] and its inheritors
            [FirRegularClass] and [FirAnonymousObject]) as well as type aliases (see [FirTypeAlias]).

            Notable properties:
            - [symbol] — the symbol which serves as a pointer to this class-like declaration.
            - [typeParameters] — type parameter references declared for this class-like declaration, if any.
            - [scopeProvider] — a provider used to get different kind of scopes, like a use-site scope, a static scope, or a nested classifier scope
            (see [FirScopeProvider], [org.jetbrains.kotlin.fir.scopes.FirScope]) for names resolution. There are two main providers used (Kotlin and Java ones).
            - [isLocal] — the class-like is non-local (isLocal = false) iff all its ancestors (containing declarations) are
            either files (see [FirFile]) or classes. With any callable or anonymous initializer among ancestors, the class-like is local (isLocal = true).
            In particular, it means that any class-like declared inside a local class is also local. 
                """.trimIndent()
        parent(memberDeclaration)
        parent(statement)
        parent(typeParameterRefsOwner)

        +declaredSymbol(classLikeSymbolType.withArgs(classLikeDeclaration))
        +field("deprecationsProvider", deprecationsProviderType, withReplace = true) {
            isMutable = true
        }
        +field("scopeProvider", firScopeProviderType)
    }

    val klass: Element by sealedElement(Declaration, name = "Class") {
        kDoc = """
            Represents a Kotlin class declaration in FIR, serving as a common supertype for concrete class kinds
            such as [FirRegularClass] and [FirAnonymousObject]. It abstracts over whether the class is named or anonymous.
            This includes similar declarations as an interface, an object (companion, named, or anonymous), 
            an enum or annotation class, but excludes a type alias.

            Notable properties:
            - [classKind] — what kind of class it is (interface, object, enum class, enum entry, annotation class, or a plain class).
            - [symbol] — the symbol which serves as a pointer to this class-like declaration.
            - [typeParameters] — the type parameters of the class and references to type parameters of its outer classes, if any 
            - [superTypeRefs] — explicitly declared supertypes, or [kotlin.Any] by default.
            - [declarations] — member declarations inside the class.
            - [annotations] — annotations present on the class, if any.
                """.trimIndent()
        parent(classLikeDeclaration)
        parent(statement)
        parent(controlFlowGraphOwner)

        +declaredSymbol(classSymbolType.withArgs(klass))
        +field(classKindType)
        +listField("superTypeRefs", typeRef, withReplace = true, withTransform = true)
        +declarations {
            withTransform = true
        }
        +annotations
    }

    val regularClass: Element by element(Declaration) {
        kDoc = """
            Represents a regular (in sense of being named) Kotlin class declaration.
            This includes similar declarations as an interface, an object, an enum or annotation class,
            but excludes an anonymous object or a type alias.

            Notable properties:
            - [name] — the simple name of the class.
            - [classKind] — what kind of class it is (interface, object, enum class, enum entry, annotation class, or a plain class). 
            - [symbol] — the symbol which serves as a pointer to the class. 
            - [typeParameters] — the type parameters of the class and references to type parameters of its outer classes, if any 
            - [superTypeRefs] — explicitly declared supertypes, or [kotlin.Any] by default.
            - [companionObjectSymbol] — Symbol of the companion object if present, otherwise null.
            - [declarations] — member declarations inside the class.
            - [annotations] — annotations present on the class, if any.
            - [hasLazyNestedClassifiers] — Whether nested classifiers are computed lazily
            (targeted for implementations that are lazy by nature, currently used for Java class implementations). 
                """.trimIndent()
        parent(klass)

        +FieldSets.name
        +declaredSymbol(regularClassSymbolType)
        +field("hasLazyNestedClassifiers", boolean)
        +referencedSymbol("companionObjectSymbol", regularClassSymbolType, nullable = true, withReplace = true)
        +listField("superTypeRefs", typeRef, withReplace = true)
        +listField(name = "contextParameters", valueParameter, useMutableOrEmpty = true, withTransform = true)
    }

    val anonymousObject: Element by element(Declaration) {
        kDoc = """
            Represents an anonymous object declaration created by an `object` expression.
            Unlike [FirRegularClass], it has no name and is always declared as a part of an expression at the usage site.

            Notable properties:
            - [classKind] — always [ClassKind.OBJECT]. 
            - [symbol] — the symbol which serves as a pointer to this anonymous object.
            - [superTypeRefs] — explicitly declared supertypes of the object literal, or [kotlin.Any] by default.
            - [isLocal] — always true for anonymous object. 
            - [declarations] — member declarations inside the anonymous object.
            - [annotations] — annotations present on the object literal, if any.
                """.trimIndent()
        parent(klass)

        +declaredSymbol(anonymousObjectSymbolType)
    }

    val anonymousObjectExpression: Element by element(Expression) {
        parent(expression)

        +field(anonymousObject, withTransform = true)
    }

    val typeAlias: Element by element(Declaration) {
        kDoc = """
            Represents a Kotlin type alias declaration in FIR.
            A type alias provides an alternative name for an existing type without introducing a new classifier.

            Notable properties:
            - [name] — the simple name of the type alias.
            - [symbol] — the symbol which serves as a pointer to this type alias.
            - [typeParameters] — type parameters referenced by the alias, if any.
            - [expandedTypeRef] — the underlying type this alias expands to.
            - [annotations] — annotations present on the type alias, if any.
                """.trimIndent()
        parent(classLikeDeclaration)

        +FieldSets.name
        +declaredSymbol(typeAliasSymbolType)
        +field("expandedTypeRef", typeRef, withReplace = true, withTransform = true)
        +annotations
    }

    val anonymousFunction: Element by element(Declaration) {
        kDoc = """
            Represents an anonymous function or lambda expression in FIR.
            Unlike [FirNamedFunction], this declaration has no name. Lambdas are represented
            as anonymous functions with [isLambda] set to `true`.

            Notable properties:
            - [typeRef] — the functional type of this anonymous function.
            - [symbol] — the symbol which serves as a pointer to this anonymous function.
            - [label] — an optional label attached to the anonymous function (e.g., `label@ { ... }`).
            - [invocationKind] — how many times the function is expected to be invoked (see [org.jetbrains.kotlin.contracts.description.EventOccurrencesRange]).
            - [inlineStatus] — information about inlining status of this function (inline, noinline, or crossinline).
            - [isLambda] — whether this anonymous function originates from a lambda expression or not.
            - [typeParameters] — type parameters declared for the anonymous function, if any.
            (always empty for a green code, but technically they can exist).
            - [hasExplicitParameterList] — whether the parameter list is explicitly specified (affects implicit `it`).
            - [valueParameters] — the list of the function's value parameters
            (for a lambda, the list can be empty at creation and filled later during resolution).
            - [contextParameters] — context parameters of the function, if any.
            - [receiverParameter] — the extension receiver parameter if present, otherwise null.
            - [returnTypeRef] — the declared return type of the function
            (if type is assumed to be inferred, [FirImplicitTypeRef] is used here).
            - [body] — the function body, if present, otherwise null.
            - [contractDescription] — contract description for the function, if present (see [FirContractDescription] and its inheritors).
            - [annotations] — annotations present on the function, if any.
            - [isLocal] — always true for anonymous functions. 
                """.trimIndent()
        parent(function)
        parent(typeParametersOwner)
        parent(contractDescriptionOwner)

        +declaredSymbol(anonymousFunctionSymbolType)
        +field(label, nullable = true)
        +field("invocationKind", eventOccurrencesRangeType, nullable = true, withReplace = true) {
            isMutable = true
        }
        +field("inlineStatus", inlineStatusType, withReplace = true) {
            isMutable = true
        }
        +field("isLambda", boolean)
        +field("hasExplicitParameterList", boolean)
        +typeParameters
        +field(typeRef, withReplace = true)
    }

    val anonymousFunctionExpression: Element by element(Expression) {
        parent(expression)

        +field(anonymousFunction, withTransform = true, withReplace = true)
        +field("isTrailingLambda", boolean, withReplace = true) {
            replaceOptInAnnotation = rawFirApi
        }
    }

    val typeParameter: Element by element(Declaration) {
        parent(typeParameterRef)
        parent(declaration)

        +FieldSets.name
        +declaredSymbol(typeParameterSymbolType)
        +referencedSymbol("containingDeclarationSymbol", firBasedSymbolType.withArgs(TypeRef.Star)) {
            withBindThis = false
        }
        +field(varianceType)
        +field("isReified", boolean)
        // TODO: `useMutableOrEmpty = true` is a workaround for KT-60324 until KT-60445 has been fixed.
        +listField("bounds", typeRef, withReplace = true, useMutableOrEmpty = true)
        +annotations
    }

    val constructedClassTypeParameterRef: Element by element(Declaration) {
        parent(typeParameterRef)
    }

    val outerClassTypeParameterRef: Element by element(Declaration) {
        parent(typeParameterRef)
    }

    val namedFunction: Element by element(Declaration) {
        kDoc = """
            Represents a named Kotlin function declaration.
            This covers top-level functions, member functions, and local named functions,
            but excludes lambdas, anonymous functions, and secondary constructors.

            Notable properties:
            - [name] — the simple name of the function.
            - [symbol] — the symbol which serves as a pointer to this function.
            - [typeParameters] — type parameters declared for the function, if any.
            - [valueParameters] — the list of the function's value parameters.
            - [dispatchReceiverType] — dispatch receiver type for member functions, or null for top-level or static functions.
            Dispatch receiver type is a type of `this` based on the member function's owner class and used to determine accessible scopes.
            - [contextParameters] — context parameters of the function, if any.
            - [receiverParameter] — the extension receiver parameter if the function is an extension, otherwise null.
            - [returnTypeRef] — the declared return type of the function.
            (if type is assumed to be inferred, [FirImplicitTypeRef] is used here).
            - [body] — the function body, if present, otherwise null.
            - [contractDescription] — contract description for the function, if present (see [FirContractDescription] and its inheritors).
            - [annotations] — annotations present on the function, if any.
                """.trimIndent()
        parent(function)
        parent(contractDescriptionOwner)
        parent(typeParametersOwner)

        +FieldSets.name
        +declaredSymbol(namedFunctionSymbolType)
        +annotations
        +typeParameters
    }

    val contractDescriptionOwner: Element by sealedElement(Declaration) {
        +field(contractDescription, withReplace = true, nullable = true, withTransform = true)
        +field("body", block, nullable = true)
        +listField("valueParameters", valueParameter)
    }

    val property: Element by element(Declaration) {
        parent(variable)
        parent(typeParametersOwner)
        parent(controlFlowGraphOwner)

        +declaredSymbol(propertySymbolType)
        +referencedSymbol("delegateFieldSymbol", delegateFieldSymbolType, nullable = true)
        +field("bodyResolveState", propertyBodyResolveStateType, withReplace = true)
        +typeParameters
    }

    val propertyAccessor: Element by element(Declaration) {
        kDoc = """
            Represents a property accessor declaration — either a getter or a setter — in FIR.
            Exactly one of [isGetter] or [isSetter] is true for a given accessor.

            Notable properties:
            - [symbol] — the symbol which serves as a pointer to this accessor.
            - [propertySymbol] — the symbol of the property this accessor belongs to.
            - [isGetter] — whether this accessor is a getter.
            - [isSetter] — whether this accessor is a setter.
            - [typeParameters] — type parameters declared for the accessor (normally empty, but so-called synthetic property accessors can have them).
            - [valueParameters] — value parameters of the accessor (for a setter, normally contains a single parameter representing the value being set; empty for a getter).
            - [dispatchReceiverType] — always null for property accessors. 
            - [receiverParameter] — the extension receiver parameter if the containing property is an extension, otherwise null.
            - [returnTypeRef] — the return type of the accessor (normally it's the property type for a getter, and [kotlin.Unit] for a setter).
            - [contextParameters] — context parameters of the accessor, if any.
            - [body] — the body of the accessor, if present, otherwise null.
            - [contractDescription] — contract description for the accessor, if present (see [FirContractDescription] and its inheritors).
            - [annotations] — annotations present on the accessor, if any.
            - [isLocal] — the property accessor is considered local iff its owner property is local.
                """.trimIndent()
        parent(function)
        parent(contractDescriptionOwner)
        parent(typeParametersOwner)

        +declaredSymbol(propertyAccessorSymbolType)
        +referencedSymbol("propertySymbol", firPropertySymbolType) {
            withBindThis = false
        }
        +field("isGetter", boolean)
        +field("isSetter", boolean)
        +annotations
        +typeParameters
    }

    val backingField: Element by element(Declaration) {
        kDoc = """
            Represents a backing field for a property.

            Notable properties:
            - [symbol] — the symbol which serves as a pointer to this backing field.
            - [propertySymbol] — the symbol of the property this backing field belongs to.
            - [returnTypeRef] — the type of the backing field.
            - [annotations] — annotations present on the backing field, if any.
            - [isLocal] — the backing field is considered local iff its owner property is local.
               """.trimIndent()
        parent(variable)
        parent(typeParametersOwner)
        parent(statement)

        +declaredSymbol(backingFieldSymbolType)
        +referencedSymbol("propertySymbol", firPropertySymbolType) {
            withBindThis = false
        }
        +field("initializer", expression, nullable = true, withReplace = true, withTransform = true)
        +annotations
        +typeParameters
        +field("status", declarationStatus, withReplace = true, withTransform = true)
    }

    val declarationStatus: Element by element(Declaration) {
        +field(visibilityType)
        +field(modalityType, nullable = true)
        generateBooleanFields(
            "expect", "actual", "override", "operator", "infix", "inline", "value", "tailRec",
            "external", "const", "lateInit", "inner", "companion", "data", "suspend", "static",
            "fromSealedClass", "fromEnumClass", "fun", "hasStableParameterNames"
        )
        +field("returnValueStatus", returnValueStatusType, nullable = false)
        +field("defaultVisibility", visibilityType, nullable = false)
        +field("defaultModality", modalityType, nullable = false)
    }

    val resolvedDeclarationStatus: Element by element(Declaration) {
        kind = ImplementationKind.Interface

        parent(declarationStatus)

        +field(modalityType, nullable = false)
        +field("effectiveVisibility", effectiveVisibilityType)
    }

    val implicitInvokeCall: Element by element(Expression) {
        parent(functionCall)

        +field("isCallWithExplicitReceiver", boolean)
    }

    val constructor: Element by element(Declaration) {
        kDoc = """
            Represents a Kotlin class constructor declaration in FIR.
            This covers both primary and secondary constructors.

            Notable properties:
            - [symbol] — the symbol which serves as a pointer to this constructor.
            - [isPrimary] — whether this constructor is the primary constructor of the class.
            - [typeParameters] — the type parameters of the constructor itself (Java only) and references to type parameters of the owner class and its outer classes, if any.
            - [valueParameters] — the list of value parameters of the constructor.
            - [dispatchReceiverType] — dispatch receiver type for inner class constructors, or null for nested class constructors.
            For inner class constructors the dispatch receiver type is bound to the outer class `this`, not to the owner class `this`.
            - [contextParameters] — context parameters of the constructor, if any.
            - [receiverParameter] — the extension receiver parameter (normally should be null as constructors cannot be extensions).
            - [returnTypeRef] — the constructed type of the enclosing class.
            - [delegatedConstructor] — the delegated constructor call (`this(...)` or `super(...)`) for secondary constructors, if present.
            For primary constructors the equivalent of `super(...)` call is built based on a given superclass.
            - [body] — the body of a secondary constructor, if present; always null for primary constructors.
            - [contractDescription] — contract description for the constructor, if present (see [FirContractDescription] and its inheritors).
            - [annotations] — annotations present on the constructor, if any.
                """.trimIndent()
        parent(function)
        parent(typeParameterRefsOwner)
        parent(contractDescriptionOwner)

        +annotations
        +declaredSymbol(constructorSymbolType)
        +field("delegatedConstructor", delegatedConstructorCall, nullable = true, withReplace = true, withTransform = true)
        +field("body", block, nullable = true)
        +field("isPrimary", boolean)
    }

    val errorPrimaryConstructor: Element by element(Declaration) {
        parent(constructor)
        parent(diagnosticHolder)
    }

    val delegatedConstructorCall: Element by element(Expression) {
        parent(resolvable)
        parent(call)
        parent(contextArgumentListOwner)
        parent(expression)

        +field("constructedTypeRef", typeRef, withReplace = true)
        +field("dispatchReceiver", expression, nullable = true, withReplace = true, withTransform = true)
        +field("calleeReference", reference, withReplace = true)
        +field("source", sourceElementType, nullable = true, withReplace = true)
        generateBooleanFields("this", "super")
    }

    val multiDelegatedConstructorCall: Element by element(Expression) {
        parent(delegatedConstructorCall)

        +listField("delegatedConstructorCalls", delegatedConstructorCall, withReplace = true, withTransform = true)
    }

    val valueParameter: Element by element(Declaration) {
        parent(variable)
        parent(controlFlowGraphOwner)

        +declaredSymbol(valueParameterSymbolType)
        +field("defaultValue", expression, nullable = true, withReplace = true)
        +referencedSymbol("containingDeclarationSymbol", firBasedSymbolType.withArgs(TypeRef.Star)) {
            withBindThis = false
        }
        generateBooleanFields("crossinline", "noinline", "vararg")
        +field("valueParameterKind", valueParameterKindType)
    }

    val receiverParameter: Element by element(Declaration) {
        parent(declaration)

        +declaredSymbol(receiverParameterSymbolType)
        +field(typeRef, withReplace = true, withTransform = true)
        +referencedSymbol("containingDeclarationSymbol", firBasedSymbolType.withArgs(TypeRef.Star)) {
            withBindThis = false
        }
        +annotations
    }

    val scriptReceiverParameter: Element by element(Declaration) {
        parent(receiverParameter)

        +field(typeRef, withReplace = true, withTransform = true)
        // means coming from ScriptCompilationConfigurationKeys.baseClass (could be deprecated soon, see KT-68540)
        +field("isBaseClassReceiver", boolean)
    }

    val variable: Element by sealedElement(Declaration) {
        needTransformOtherChildren()

        parent(callableDeclaration)
        parent(statement)

        +FieldSets.name
        +declaredSymbol(variableSymbolType.withArgs(variable))
        +field("initializer", expression, nullable = true, withReplace = true, withTransform = true)
        +field("delegate", expression, nullable = true, withReplace = true, withTransform = true)
        generateBooleanFields("var", "val")
        +field("getter", propertyAccessor, nullable = true, withReplace = true, withTransform = true)
        +field("setter", propertyAccessor, nullable = true, withReplace = true, withTransform = true)
        +field("backingField", backingField, nullable = true, withTransform = true)
        +annotations
    }

    val functionTypeParameter: Element by element(Other) {
        parent(rootElement)

        +field("source", sourceElementType, nullable = false)
        +field("name", nameType, nullable = true)
        +field("returnTypeRef", typeRef)
    }

    val errorProperty: Element by element(Declaration) {
        parent(property)
        parent(diagnosticHolder)

        +declaredSymbol(errorPropertySymbolType)
    }

    val enumEntry: Element by element(Declaration) {
        parent(variable)

        +declaredSymbol(enumEntrySymbolType)
    }

    val field: Element by element(Declaration) {
        parent(variable)
        parent(controlFlowGraphOwner)

        +declaredSymbol(fieldSymbolType)
        generateBooleanFields("hasConstantInitializer")
    }

    val anonymousInitializer: Element by element(Declaration) {
        parent(declaration)
        parent(controlFlowGraphOwner)

        +field("body", block, nullable = true, withReplace = true)
        +declaredSymbol(anonymousInitializerSymbolType)
        +referencedSymbol("containingDeclarationSymbol", firBasedSymbolType.withArgs(TypeRef.Star)) {
            withBindThis = false
        }
    }

    val danglingModifierList: Element by element(Declaration) {
        parent(declaration)
        parent(diagnosticHolder)

        +declaredSymbol(danglingModifierSymbolType)
        +listField(name = "contextParameters", valueParameter, useMutableOrEmpty = true, withReplace = true, withTransform = true)
    }

    val file: Element by element(Declaration) {
        parent(declaration)
        parent(controlFlowGraphOwner)

        +field("packageDirective", packageDirective)
        +listField(import, withTransform = true)
        +declarations {
            withTransform = true
        }
        +field("name", string)
        +field("sourceFile", sourceFileType, nullable = true)
        +field("sourceFileLinesMapping", sourceFileLinesMappingType, nullable = true)
        +declaredSymbol(fileSymbolType)
    }

    val script: Element by element(Declaration) {
        parent(declaration)
        parent(controlFlowGraphOwner)

        +FieldSets.name
        +declarations {
            withTransform = true
            withReplace = true
        }
        +field("source", sourceElementType, nullable = false)
        +declaredSymbol(scriptSymbolType)
        +listField("parameters", property, withTransform = true)
        +listField("receivers", scriptReceiverParameter, useMutableOrEmpty = true, withTransform = true)
        +field("resultPropertyName", nameType, nullable = true)
    }

    val codeFragment: Element by element(Declaration) {
        parent(declaration)

        +declaredSymbol(codeFragmentSymbolType)
        +field(block, withReplace = true, withTransform = true)
    }

    val replSnippet: Element by element(Declaration) {
        parent(declaration)
        parent(controlFlowGraphOwner)

        +declaredSymbol(replSnippetSymbolType)

        +field("source", sourceElementType, nullable = false)
        +listField("receivers", scriptReceiverParameter, useMutableOrEmpty = true, withTransform = true)
        +field("snippetClass", regularClass, withTransform = true)
        +field("evalFunctionName", nameType)
    }

    val replDeclarationReference: Element by element(Expression) {
        parent(statement)

        +referencedSymbol("symbol", firBasedSymbolType.withArgs(TypeRef.Star)) {
            withBindThis = false
            isMutable = false
        }
    }

    val packageDirective: Element by element(Other) {
        +field("packageFqName", fqNameType)
    }

    val import: Element by element(Declaration) {
        kind = ImplementationKind.AbstractClass

        +field("importedFqName", fqNameType, nullable = true)
        +field("isAllUnder", boolean)
        +field("isPackage", boolean)
        +field("aliasName", nameType, nullable = true)
        +field("aliasSource", sourceElementType, nullable = true)
    }

    val resolvedImport: Element by element(Declaration) {
        parent(import)

        +field("delegate", import, isChild = false)
        +field("packageFqName", fqNameType)
        +field("relativeParentClassName", fqNameType, nullable = true)
        +field("resolvedParentClassId", classIdType, nullable = true)
        +field("importedName", nameType, nullable = true)
    }

    val annotation: Element by element(Expression) {
        parent(expression)

        +field("useSiteTarget", annotationUseSiteTargetType, nullable = true, withReplace = true)
        +field("annotationTypeRef", typeRef, withReplace = true, withTransform = true)
        +field("argumentMapping", annotationArgumentMapping, withReplace = true)
        +typeArguments {
            withTransform = true
        }
    }

    val annotationCall: Element by element(Expression) {
        parent(annotation)
        parent(call)
        parent(resolvable)

        +field("argumentMapping", annotationArgumentMapping, withReplace = true, isChild = false)
        +field("annotationResolvePhase", annotationResolvePhaseType, withReplace = true)
        +referencedSymbol("containingDeclarationSymbol", firBasedSymbolType.withArgs(TypeRef.Star)) {
            withBindThis = false
        }
    }

    val errorAnnotationCall: Element by element(Expression) {
        parent(annotationCall)
        parent(diagnosticHolder)

        +field("argumentMapping", annotationArgumentMapping, withReplace = true, isChild = false)
    }

    val annotationArgumentMapping: Element by element(Expression) {
        +field("mapping", StandardTypes.map.withArgs(nameType, expression))
    }

    val indexedAccessAugmentedAssignment: Element by element(Expression) {
        kDoc = """
                    Represents an augmented assignment with an indexed access as the receiver (e.g., `arr[i] += 1`)
                    **before** it gets resolved.
                    
                    After resolution, the call will be desugared into regular function calls,
                    either of the form `arr.set(i, arr.get(i).plus(1))` or `arr.get(i).plusAssign(1)`.
                """.trimIndent()

        parent(statement)

        +field("lhsGetCall", functionCall)
        +field("rhs", expression)
        +field("operation", operationType)
        // Used for resolution errors reporting in case
        +field("calleeReference", reference, withReplace = true)
        +field("arrayAccessSource", sourceElementType, nullable = true)
    }

    val classReferenceExpression: Element by element(Expression) {
        parent(expression)

        +field("classTypeRef", typeRef)
    }

    val componentCall: Element by element(Expression) {
        parent(functionCall)

        +field("explicitReceiver", expression)
        +field("componentIndex", int)
    }

    val smartCastExpression: Element by element(Expression) {
        parent(expression)

        +field("originalExpression", expression, withReplace = true, withTransform = true)
        +field("upperTypesFromSmartCast", StandardTypes.collection.withArgs(coneKotlinTypeType))
        +field("lowerTypesFromSmartCast", StandardTypes.collection.withArgs(dfaType))
        +field("smartcastType", typeRef)
        +field("smartcastTypeWithoutNullableNothing", typeRef, nullable = true)
        +field("isStable", boolean)
        +field(smartcastStabilityType)
    }

    val safeCallExpression: Element by element(Expression) {
        parent(expression)

        +field("receiver", expression, withTransform = true)
        // Special node that might be used as a reference to receiver of a safe call after null check
        +field("checkedSubjectRef", safeCallCheckedSubjectReferenceType)
        // One that uses checkedReceiver as a receiver
        +field("selector", statement, withReplace = true, withTransform = true)
    }

    val checkedSafeCallSubject: Element by element(Expression) {
        parent(expression)

        +field("originalReceiverRef", referenceToSimpleExpressionType)
    }

    val callableReferenceAccess: Element by element(Expression) {
        parent(qualifiedAccessExpression)

        +field("calleeReference", namedReference, withReplace = true, withTransform = true)
        +field("hasQuestionMarkAtLHS", boolean, withReplace = true)
    }

    val propertyAccessExpression: Element by element(Expression) {
        parent(qualifiedAccessExpression)
        +field("calleeReference", namedReference, withReplace = true, withTransform = true)
    }

    val getClassCall: Element by element(Expression) {
        parent(expression)
        parent(call)

        +field("argument", expression)
    }

    val wrappedArgumentExpression: Element by sealedElement(Expression) {
        parent(wrappedExpression)

        +field("isSpread", boolean)
    }

    val spreadArgumentExpression: Element by element(Expression) {
        kDoc = """
                |### Up to and including body resolution phase
                |
                |Represents a spread expression `*foo`. If a spread expression is passed as named argument `foo = *bar`, it will be
                |represented as an [${namedArgumentExpression.render()}] with [${namedArgumentExpression.render()}.isSpread] set to `true`.
                |  
                |### After body resolution phase
                |
                |Represents spread expressions `*foo` and named argument expressions for vararg parameters `foo = bar` and `foo = *bar`.
                |
                |If [isNamed] is `true`, it means the argument was passed in named form. The name is not saved since it's not required.
                |To retrieve the argument mapping of a call, [${firResolvedArgumentListType.render()}.mapping] must be used.
                |
                |If [isFakeSpread] is `true`, it means this expression is the argument to a `vararg` parameter that was passed in named form
                |without a spread operator `*`.
                |
                |The information carried by [isNamed] and [isFakeSpread] is only relevant for some checkers. Otherwise,
                |[FirSpreadArgumentExpression]s should be treated uniformly since they always represent an array that was passed to a
                |`vararg` parameter and don't influence the resulting platform code.
            """.trimMargin()

        parent(wrappedArgumentExpression)

        +field("isNamed", boolean)
        +field("isFakeSpread", boolean)
    }

    val namedArgumentExpression: Element by element(Expression) {
        kDoc = """
                |Represents a named argument `foo = bar` before and during body resolution phase.
                |
                |After body resolution, all [${namedArgumentExpression.render()}]s are removed from the FIR tree and the argument mapping must be
                |retrieved from [${firResolvedArgumentListType.render()}.mapping].
                |
                |For a named argument with spread operator `foo = *bar`, [isSpread] will be set to `true` but no additional
                |[${spreadArgumentExpression.render()}] will be created as the [expression].
                |
                |**Special case vor varargs**: named arguments for `vararg` parameters are replaced with [${spreadArgumentExpression.render()}] with
                |[${spreadArgumentExpression.render()}.isNamed] set to `true`.
                |
                |See [${varargArgumentsExpression.render()}] for the general structure of arguments of `vararg` parameters after resolution.
            """.trimMargin()

        parent(wrappedArgumentExpression)

        +FieldSets.name
    }

    val varargArgumentsExpression: Element by element(Expression) {
        parent(expression)

        +listField("arguments", expression)
        +field("coneElementTypeOrNull", coneKotlinTypeType, nullable = true)

        kDoc = """
                |[${varargArgumentsExpression.render()}]s are created during body resolution phase for arguments of `vararg` parameters.
                |
                |If one or multiple elements are passed to a `vararg` parameter, they will be wrapped with a [${varargArgumentsExpression.render()}]
                |and [arguments] will contain the individual elements.
                |
                |If a named argument is passed to a `vararg` parameter, [arguments] will contain a single [${spreadArgumentExpression.render()}]
                |with [${spreadArgumentExpression.render()}.isNamed] set to `true`.
                |
                |[${spreadArgumentExpression.render()}]s are kept as is in [arguments]. 
                |
                |If no element is passed to a `vararg` parameter, no [${varargArgumentsExpression.render()}] is created regardless of whether the
                |parameter has a default value.
            """.trimMargin()
    }

    val samConversionExpression: Element by element(Expression) {
        parent(expression)

        +field("expression", expression)
        +field("usesFunctionKindConversion", boolean)
    }

    val resolvedQualifier: Element by element(Expression) {
        parent(expression)

        +field("packageFqName", fqNameType)
        +field("relativeClassFqName", fqNameType, nullable = true)
        +field("classId", classIdType, nullable = true)
        +referencedSymbol("symbol", classLikeSymbolType, nullable = true)
        +field("explicitParent", resolvedQualifier, nullable = true)
        +field("isNullableLHSForCallableReference", boolean, withReplace = true)
        +field("resolvedToCompanionObject", boolean, withReplace = true)
        +field("canBeValue", boolean, withReplace = true) {
            kDoc = "If true, the qualifier is resolved to an object or companion object and can be used as an expression."
        }
        +field("isFullyQualified", boolean)
        +listField("nonFatalDiagnostics", coneDiagnosticType, useMutableOrEmpty = true)
        +field("resolvedSymbolOrigin", resolvedSymbolOrigin, nullable = true, withReplace = true)
        +typeArguments {
            withTransform = true
        }
    }

    val errorResolvedQualifier: Element by element(Expression) {
        parent(resolvedQualifier)
        parent(diagnosticHolder)
    }

    val resolvedReifiedParameterReference: Element by element(Expression) {
        parent(expression)

        +referencedSymbol(typeParameterSymbolType)
    }

    val stringConcatenationCall: Element by element(Expression) {
        parent(call)
        parent(expression)

        +field("interpolationPrefix", string)
        +field("isFoldedStrings", boolean)
    }

    val throwExpression: Element by element(Expression) {
        parent(expression)

        +field("exception", expression, withTransform = true)
    }

    val variableAssignment: Element by element(Expression) {
        parent(statement)

        +field("lValue", expression, withReplace = true, withTransform = true)
        +field("rValue", expression, withTransform = true)
    }

    val whenSubjectExpression: Element by element(Expression) {
        parent(propertyAccessExpression)
    }

    val desugaredAssignmentValueReferenceExpression: Element by element(Expression) {
        parent(expression)

        +field("expressionRef", referenceToSimpleExpressionType)
    }

    val wrappedExpression: Element by element(Expression) {
        parent(expression)

        +field(expression)
    }

    val wrappedDelegateExpression: Element by element(Expression) {
        parent(wrappedExpression)

        +field("provideDelegateCall", functionCall)
    }

    val enumEntryDeserializedAccessExpression: Element by element(Expression) {
        parent(expression)

        +field("enumClassId", classIdType)
        +field("enumEntryName", nameType)
    }

    val reference: Element by element(Reference) {
        kind = ImplementationKind.AbstractClass
    }

    val namedReference: Element by element(Reference) {
        parent(reference)

        +FieldSets.name
    }

    val namedReferenceWithCandidateBase: Element by element(Reference) {
        parent(namedReference)

        +referencedSymbol("candidateSymbol", firBasedSymbolType.withArgs(TypeRef.Star))
    }

    val resolvedNamedReference: Element by element(Reference) {
        parent(namedReference)

        +referencedSymbol("resolvedSymbol", firBasedSymbolType.withArgs(TypeRef.Star))
        +field("resolvedSymbolOrigin", resolvedSymbolOrigin, nullable = true, withReplace = true)
    }

    val propertyWithExplicitBackingFieldResolvedNamedReference: Element by element(Reference) {
        parent(resolvedNamedReference)

        +field("hasVisibleBackingField", boolean)
    }

    val resolvedCallableReference: Element by element(Reference) {
        parent(resolvedNamedReference)

        +listField("inferredTypeArguments", coneKotlinTypeType)
        +field("mappedArguments", callableReferenceMappedArgumentsType.withArgs(expression))
    }

    val delegateFieldReference: Element by element(Reference) {
        parent(resolvedNamedReference)

        +referencedSymbol("resolvedSymbol", delegateFieldSymbolType)
    }

    val backingFieldReference: Element by element(Reference) {
        parent(resolvedNamedReference)

        +referencedSymbol("resolvedSymbol", backingFieldSymbolType)
    }

    val superReference: Element by element(Reference) {
        parent(reference)

        +field("labelName", string, nullable = true)
        +field("superTypeRef", typeRef, withReplace = true)
    }

    val thisReference: Element by element(Reference) {
        parent(reference)

        +field("labelName", string, nullable = true)
        +referencedSymbol("boundSymbol", firThisOwnerSymbolType.withArgs(TypeRef.Star), nullable = true, withReplace = true)
        +field("isImplicit", boolean)
        +field("diagnostic", coneDiagnosticType, nullable = true, withReplace = true)
    }

    val controlFlowGraphReference: Element by element(Reference) {
        parent(reference)
    }

    val typeRef: Element by element(TypeRefElement) {
        parent(annotationContainer)

        +annotations
        +field("customRenderer", boolean)
    }

    val resolvedTypeRef: Element by element(TypeRefElement) {
        parent(typeRef)
        parent(typeRefMarkerType)

        +field("coneType", coneKotlinTypeType)
        +field("delegatedTypeRef", typeRef, nullable = true, isChild = false)
        +field("resolvedSymbolOrigin", resolvedSymbolOrigin, nullable = true, withReplace = true)
    }

    val unresolvedTypeRef: Element by sealedElement(TypeRefElement) {
        parent(typeRef)

        +field("source", sourceElementType, nullable = false)
        +field("isMarkedNullable", boolean)
    }

    val userTypeRef: Element by element(TypeRefElement) {
        parent(unresolvedTypeRef)

        +listField("qualifier", firQualifierPartType)
    }

    val functionTypeRef: Element by element(TypeRefElement) {
        parent(unresolvedTypeRef)

        +field("receiverTypeRef", typeRef, nullable = true)
        +listField("parameters", functionTypeParameter)
        +field("returnTypeRef", typeRef)
        +field("isSuspend", boolean)
        +listField("contextParameterTypeRefs", typeRef)
    }

    val dynamicTypeRef: Element by element(TypeRefElement) {
        parent(unresolvedTypeRef)
    }

    val implicitTypeRef: Element by element(TypeRefElement) {
        parent(typeRef)
    }

    val errorTypeRef: Element by element(TypeRefElement) {
        parent(resolvedTypeRef)
        parent(diagnosticHolder)

        +field("partiallyResolvedTypeRef", typeRef, nullable = true, withTransform = true)
    }

    val resolvedErrorReference: Element by element(Reference) {
        customParentInVisitor = resolvedNamedReference

        parent(resolvedNamedReference)
        parent(diagnosticHolder)
    }

    val errorNamedReference: Element by element(Reference) {
        parent(namedReference)
        parent(diagnosticHolder)
    }

    val errorSuperReference: Element by element(Reference) {
        parent(superReference)
        parent(diagnosticHolder)
    }

    val intersectionTypeRef: Element by element(TypeRefElement) {
        parent(unresolvedTypeRef)

        +field("leftType", typeRef)
        +field("rightType", typeRef)
    }

    val thisReceiverExpression: Element by element(Expression) {
        parent(qualifiedAccessExpression)

        +field("calleeReference", thisReference)
        +field("isImplicit", boolean)
    }

    val superReceiverExpression: Element by element(Expression) {
        parent(qualifiedAccessExpression)

        +field("calleeReference", superReference)
    }

    val inaccessibleReceiverExpression: Element by element(Expression) {
        parent(expression)
        parent(resolvable)

        +field("calleeReference", thisReference)
        +field("kind", inaccessibleReceiverKindType)
    }

    val whenExpression: Element by element(Expression) {
        needTransformOtherChildren()

        parent(expression)
        parent(resolvable)

        +field("subjectVariable", variable, nullable = true, withTransform = true)
        +listField("branches", whenBranch, withTransform = true)
        +field("exhaustivenessStatus", exhaustivenessStatusType, nullable = true, withReplace = true)
        +field("usedAsExpression", boolean)
    }

    val typeProjection: Element by sealedElement(TypeRefElement)

    val typeProjectionWithVariance: Element by element(TypeRefElement) {
        parent(typeProjection)

        +field(typeRef)
        +field(varianceType)
    }

    val starProjection: Element by element(TypeRefElement) {
        parent(typeProjection)
    }

    val placeholderProjection: Element by element(TypeRefElement) {
        parent(typeProjection)
    }

    val contractElementDeclaration: Element by element(Contracts) {
        +field("effect", coneContractElementType)
    }

    val effectDeclaration: Element by element(Contracts) {
        parent(contractElementDeclaration)

        +field("effect", coneEffectDeclarationType)
    }

    val contractDescription: Element by sealedElement(Contracts)

    val rawContractDescription: Element by element(Contracts) {
        parent(contractDescription)

        +listField("rawEffects", expression)
    }

    val resolvedContractDescription: Element by element(Contracts) {
        parent(contractDescription)

        +listField("effects", effectDeclaration)
        +listField("unresolvedEffects", contractElementDeclaration)
        +field("diagnostic", coneDiagnosticType, nullable = true)
    }

    val legacyRawContractDescription: Element by element(Contracts) {
        parent(contractDescription)

        +field("contractCall", functionCall)
        +field("diagnostic", coneDiagnosticType, nullable = true)
    }

    val lazyContractDescription: Element by element(Contracts) {
        kDoc = """
            A contract description in the psi2fir lazy mode.
            
            The description might represent [FirLegacyRawContractDescription] or **null** contract.
            The description has to be unwrapped before the contract phase.
            
            @see org.jetbrains.kotlin.fir.expressions.FirLazyBlock
            @see org.jetbrains.kotlin.fir.expressions.FirLazyExpression
        """.trimIndent()
        parent(legacyRawContractDescription)
    }

    val errorContractDescription: Element by element(Contracts) {
        kDoc = """
                |Represents a contract description that could not be resolved.
                |
                |Contract descriptions where the effects are unresolved are handled by [resolvedContractDescription], this type
                |is specifically for cases where the resolution fails in its entirety.
               """.trimMargin()
        parent(contractDescription)

        +field("diagnostic", coneDiagnosticType, nullable = true)
    }

    private object FieldSets {
        val typeArguments = fieldSet(listField("typeArguments", typeProjection, useMutableOrEmpty = true, withReplace = true))

        val declarations = fieldSet(
            listField(declaration) {
                useInBaseTransformerDetection = false
                optInAnnotation = directDeclarationsAccessAnnotation
            }
        )

        val annotations = fieldSet(
            listField("annotations", annotation, withReplace = true, useMutableOrEmpty = true, withTransform = true) {
                needTransformInOtherChildren = true
            }
        )

        val typeParameters = fieldSet(listField("typeParameters", typeParameter))

        val name = fieldSet(field(nameType))
    }
}
