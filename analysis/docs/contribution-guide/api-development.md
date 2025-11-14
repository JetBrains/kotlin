# API Development

This guide covers principles and practices for developing new Analysis API endpoints, including design patterns, implementation guidelines,
and documentation standards.

For information about the Kotlin Analysis API itself, check the [Analysis API documentation](https://kotl.in/analysis-api).  
For information about evolving existing APIs, see the [API Evolution Guide](api-evolution.md).

## Important Disclaimer

Not all existing parts of the Analysis API strictly follow every guideline in this document.
There are several reasons for these deviations:

- **Historical reasons**  
  Some APIs were designed before these guidelines were established and cannot be changed without breaking compatibility.
- **Performance optimizations**  
  Certain implementation details may violate ideal design principles to achieve necessary performance characteristics.
- **Platform requirements**  
  Integration with IntelliJ IDEA and other platforms sometimes requires compromises in the API design.
- **Experimental evolution**  
  Some unstable APIs intentionally explore alternative approaches before settling on final designs.

When working with existing code, strive to follow these guidelines for new APIs while respecting the constraints of existing
implementations. During refactoring, gradually align older code with these guidelines where possible without breaking compatibility.

## Core Design Principles

> [!TIP]
> Build APIs that solve real problems elegantly, prioritize Kotlin idioms, and maintain consistency with language concepts.

When designing or refactoring any part of the Analysis API, the following fundamental principles guide our decisions.

### Solve Real Problems

Start by understanding how developers currently tackle the problem you're addressing. Research existing use cases and identify common
patterns in workarounds. This ensures your API addresses actual needs rather than theoretical scenarios.
Use the [endpoint card](endpoint-card.md) when working with complicated endpoints.

### Design for Common Patterns

Provide simple, intuitive solutions for frequent use cases while offering sophisticated APIs for complex scenarios.
You can apply different stability guarantees to each tier.

```kotlin
// Simple API for common cases: get just the target symbol
expression.mainReference.resolveToSymbol()

// Advanced API for complex scenarios: get all information about the call
expression.resolveToCall()?.singleFunctionCallOrNull()?.argumentMapping
```

### Dogfood Your API

Test your API design by implementing it in real scenarios.
This reveals usability issues and missing functionality early.
If you find yourself writing wrapper functions or complex workarounds, revisit the design.

### Embrace Kotlin Idioms

Design APIs that feel natural in Kotlin code.
Use Kotlin's language features to create convenient and safe endpoints.

```kotlin
// Leverage extension functions for better readability
val KaClassSymbol.allSuperTypes: Sequence<KaType>       // Good
fun allSuperTypes(symbol: KaClassSymbol): Sequence<KaType>  // Less idiomatic

// Use Kotlin function types instead of Java interfaces
fun processTypes(filter: (KaType) -> Boolean)  // Good
fun processTypes(filter: Predicate<KaType>)    // Less idiomatic
```

### Expose Semantic Concepts

The Analysis API reflects semantic meaning, not syntactic structure (for which there is the Kotlin PSI).
If you still need to expose syntactic information, use clear naming conventions, e.g., `isMarkedThing` or `sourceThing`.

```kotlin
// Checking semantic visibility (considering all modifiers and context)
val KaSymbol.visibility: KaSymbolVisibility

// Checking the presence of a syntactic marker for nullable types (e.g., 'T?')
val KaType.isMarkedNullable: Boolean  // Not just 'isNullable'
```

### Use Established Patterns

Align with widely recognized conventions from Kotlin stdlib and the ecosystem.

```kotlin
// Reuse standard interfaces instead of custom wrappers where appropriate
interface KaAnnotationList : List<KaAnnotation>, KaLifetimeOwner {
    operator fun contains(classId: ClassId): Boolean
    operator fun get(classId: ClassId): List<KaAnnotation>
}

// Use conventional method names
interface KaType {
    override fun toString(): String  // Not 'debugString()' or 'asString()'
}
```

### Use Familiar Concepts

Align terminology and concepts with official Kotlin documentation, [language specification](https://kotlinlang.org/spec/introduction.html),
and [KEEP](https://github.com/Kotlin/KEEP)s. Avoid direct mirroring compiler internals if they don't match user mental models.

## API Guidelines

> [!TIP]
> APIs require attention to detail and forward-thinking design. Carefully design entity hierarchies, check whether endpoints are
> evolution-friendly, and provide comprehensive documentation together with tests.

### Enable Explicit API Mode

Always enable [explicit API mode](https://kotlinlang.org/docs/whatsnew14.html#explicit-api-mode-for-library-authors) to ensure all public
declarations have explicit visibility modifiers and return types. This prevents accidental API surface expansion.

```kotlin
abstract class KaDefinitelyNotNullType : KaType {  // Visibility must be specified
    final override val isNullable  // Return type must be specified
        get() = withValidityAssertion { KaTypeNullability.NON_NULLABLE }
}
```

> [!NOTE]
> To keep examples clean and focused, the `public` modifier is omitted in most code snippets.

### Maintain Binary Compatibility

Use the [binary-compatibility-validator](https://github.com/Kotlin/binary-compatibility-validator) tool to catch breaking changes.
The API must remain binary-compatible across minor versions.

### Avoid experimental language features

Stick to stable Kotlin language features in generally available Analysis API endpoints.
Experimental features may change syntax or semantics between Kotlin versions, requiring API clients to opt into unstable language features,
or may be removed completely.

When exposing an experimental feature for early adopters, always annotate affected declarations with an
[opt-in annotation](#guard-api-endpoints-with-annotations).  

### Avoid Compiler-Specific Concepts

Don't expose internal compiler representations directly.
Create abstractions that have predictable backward compatibility and make sense to API users.

```kotlin
// Bad: Exposing internal enum from compiler
interface KaConstantValue {
    val constantValueKind: ConstantValueKind  // Internal compiler enum can be changed at any time
}

// Good: API-specific abstraction
sealed interface KaConstantValue {
    val value: Any?

    interface IntValue : KaConstantValue {
        override val value: Int
    }
    interface StringValue : KaConstantValue {
        override val value: String
    }
    // ...
}
```

### Class and Interface Design

#### Prefer Interfaces to Classes

Interfaces provide more flexibility for implementations and have better binary compatibility characteristics than classes.
Use abstract classes only when you need to share implementation details or state.

```kotlin
// Preferred: Interface allows for context-tailored implementations
interface KaType : KaLifetimeOwner, KaAnnotated

// Avoid unless needed: Class restricts implementation
abstract class KaType : KaLifetimeOwner, KaAnnotated
```

#### Avoid Sophisticated Hierarchies Unless They Add Clear Value

Complex inheritance structures increase cognitive load and maintenance burden.
Consider alternative approaches before creating deep hierarchies.

Avoid marker interfaces created solely for member sharing.
Rule of thumb — ask yourself, "Would it make sense for clients to pass instances of this type around"?
If not, the interface might be unnecessary.

```kotlin
// Good: Clear, purposeful hierarchy
interface KaSymbol
interface KaDeclarationSymbol : KaSymbol {
    val modality: KaSymbolModality
    val visibility: KaSymbolVisibility
    val isExpect: Boolean
    val isActual: Boolean
}

// Bad: Unnecessary complexity
interface KaSymbol
interface KaSymbolWithModality : KaSymbol {
    val modality: KaSymbolModality
}
interface KaSymbolWithVisibility : KaSymbol {
    val visibility: KaSymbolVisibility
}
interface KaMultiplatformSymbol : KaSymbol {
    val isExpect: Boolean
    val isActual: Boolean
}
interface KaDeclarationSymbol : KaSymbolWithModality, KaSymbolWithVisibility, KaMultiplatformSymbol
```

#### Avoid Mixing Conceptual Hierarchies

Keep different concepts separate in your type hierarchies.
Prefer composition to inheritance.

Such as, a class symbol should not also be a scope, even if it contains declarations:

```kotlin
// Bad: Mixing concepts
interface KaClassSymbol : KaSymbol, KaScope

// Good: Separate concepts with clear relationships
// Even better: move out the scope API to a session component
interface KaClassSymbol : KaSymbol {
    val staticScope: KaScope
    val memberScope: KaScope
}
```

#### Use Appropriate Suffixes for Containment Interfaces

When you do need interfaces for sharing members, use clear suffixes that indicate the relationship:

```kotlin
// "Owner" for ownership relations
interface KaContextParameterOwnerSymbol : KaFunctionSymbol {
    val contextParameters: List<KaContextParameterSymbol>
}

// "Container" for weaker containment relations  
interface KaDeclarationContainerSymbol : KaSymbol {
    val declarations: Sequence<KaDeclarationSymbol>
}
```

#### Think Twice Before Introducing Sealed Classes or Enums

Sealed classes and enums create strong API commitments because you cannot add new subtypes without breaking compatibility (exhaustive
`when`). Consider your evolution needs carefully.

```kotlin
// Good: Sealed class with escape hatch for evolution
sealed class KaAnnotationValue {
    data class IntValue(val value: Int) : KaAnnotationValue()
    data class StringValue(val value: String) : KaAnnotationValue()

    // A private subclass forces 'else' branches in client code
    private class UnknownValue : KaAnnotationValue()
}

// Alternative: Statics for more flexibility (and no exhaustive 'when')
class KaSeverity private constructor(val name: String) {
    companion object {
        val ERROR: KaSeverity = KaSeverity("ERROR")
        val WARNING: KaSeverity = KaSeverity("WARNING")
    }
}
```

#### Choose Sealed Class Placement Consciously

For simpler hierarchies, prefer placing inheritors inside a sealed `class` or `interface`.
This way, users can immediately see all subtypes at a glance, and subtypes can have simpler names.

Note that with the
emerging [context-sensitive resolution](https://github.com/Kotlin/KEEP/blob/improved-resolution-expected-type/proposals/context-sensitive-resolution.md)
feature, specifying outer class names won't be needed in many cases.

```kotlin
sealed class KaSeverity {
    object Error : KaSeverity()
    object Warning : KaSeverity()
    private object Unknown : KaSeverity()  // Forces 'else' branches in exhaustiveness checks
}
```

When you have two or more inheritance levels in a sealed hierarchy, or when individual classes are meant to be passed around independently,
implement them as top-level classes:

```kotlin
sealed class KaClassifierSymbol : KaDeclarationSymbol()
sealed class KaClassSymbol : KaClassifierSymbol()
abstract class KaNamedClassSymbol : KaClassSymbol()
abstract class KaAnonymousObjectSymbol : KaClassSymbol()
abstract class KaTypeAliasSymbol : KaClassifierSymbol()
```

#### Use `@SubclassOptInRequired` for Non-Extendable Types

If an interface or class isn't intended to be subclassed by clients, make this explicit:

```kotlin
// Clients aren't supposed to create their session implementations
@SubclassOptInRequired(KaImplementationDetail::class)
interface KaSession
```

On the other hand, if you expect users to create subtypes of your class or interface,
mark the declaration with `@KaExtensibleApi` annotation and provide detailed instructions on not only how to use
each of the members, but also how to implement them correctly.

#### Check Whether Your Entity Needs to be a `KaLifetimeOwner`

Entities containing symbols or types typically
require [lifetime management](https://kotlin.github.io/analysis-api/fundamentals.html#kalifetimeowner).
Consider how your entity will be used outside analysis sessions.

```kotlin
// Needs lifetime management: Contains symbols
interface KaCallInfo : KaLifetimeOwner {
    val targetFunction: KaFunctionSymbol  // Symbol requires lifetime
}

// Doesn't need lifetime management: Pure data
sealed interface KaConstantValue {
    val value: Any?
    val sourcePsi: KtElement?

    fun render(): String
}
```

#### Avoid Data Classes in Public APIs

Data classes [aren't designed](https://kotlinlang.org/docs/api-guidelines-backward-compatibility.html#avoid-using-data-classes-in-your-api)
for API evolution. They expose implementation details through generated methods and make compatibility harder to maintain.

```kotlin
// Bad: Data class in public API
data class KaCallInfo(
    val targetFunction: KaFunctionSymbol,
    val arguments: List<KaArgument>
)

// Good: Interface with explicit contract
interface KaCallInfo {
    val targetFunction: KaFunctionSymbol
    val arguments: List<KaArgument>
}
```

#### Document Equality Semantics

If your type overrides `equals()` and `hashCode()`, clearly specify the equality contract.

Ensure the contract confirms to the Java requirements for
[`Object.equals()`](https://docs.oracle.com/en/java/javase/24/docs/api//java.base/java/lang/Object.html#equals(java.lang.Object)) and
[`Object.hashCode()`](https://docs.oracle.com/en/java/javase/24/docs/api//java.base/java/lang/Object.html#hashCode()). If conformance is
impossible, document the reasons.

```kotlin
/**
 * [KaType.equals] implements *structural type equality*, which may not match
 * with the usual intuition of type equality. Structural equality
 * is favored for `equals` because it is fast and predictable, and
 * additionally it allows constructing a hash code.
 *
 * For semantic type comparisons, [semanticallyEquals] should be used, as it
 * implements the equality rules defined by the type system.
 */
interface KaType
```

### Property and Function Design

> [!TIP]
> Design properties and functions that are intuitive, evolution-friendly, and follow Kotlin conventions. Pay attention to parameter
> names, return types, and error handling.

#### Choose Between Properties and Functions Based on Semantics

In many languages, properties are often used for simple operations that don't involve expensive computation, or when the result is already
known, while functions represent more complex operations. However, as the Analysis API features on-demand code analysis, it's rarely
possible to predict the amount of computation required for a given endpoint.

So instead, choose between properties and functions based on the endpoint semantic, and aesthetic of its usages:

- Use functions when you consume parameters. While in such cases there's typically no choice in the first place, think of whether an
  extension property will look more elegant.
- Prefer functions for **actions** – an endpoint that performs some work *conceptually* (e.g., type creation) or has side effects.
- Prefer properties for semantical **attributes** that users just *get* or *set*. E.g., `KtClassOrObject.symbol` means
  "get a symbol that represents the given class". The user doesn't care whether the symbol is newly computed or taken from the cache.

```kotlin
// Property: No user-visible side effects
val KaClassSymbol.isData: Boolean

// Function: May involve computation or have parameters
fun KtElement.resolveToCall(): KaCallInfo?
fun KaClassSymbol.isSubClassOf(superClass: KaClassSymbol): Boolean
```

#### Provide Convenience Properties for Simple Cases

If you have a function endpoint with a parameter which is often omitted (for instance, by providing a trivial value, such as `null`, an
empty collection, or a `true`-returning lambda), consider adding a convenience property with the same name:

```kotlin
val KaScope.callables: Sequence<KaCallableSymbol>
fun KaScope.callables(nameFilter: (Name) -> Boolean): Sequence<KaCallableSymbol>
```

#### Avoid Unsafe Defaults

On incorrect or incomplete input, the Kotlin compiler may return a partial resolution result. Never assume a reference or a call can
always be resolved.

Don't expose an API relying on unsafe assumptions:

```kotlin
// Bad: Throws on analysis failure
fun KtCallExpression.resolveSymbol(): KaFunctionSymbol  // Throws if resolution fails

// Good: Returns 'null' on analysis failure
fun KtCallExpression.resolveSymbol(): KaFunctionSymbol?
```

#### Avoid Exceptions for Non-Exceptional Cases

Use appropriate return types to indicate absence or failure:

```kotlin
// Bad: Throwing for missing elements
fun getClass(classId: ClassId): KaClassSymbol  // Throws if not found

// Good: Null for not found
fun findClass(classId: ClassId): KaClassSymbol?

// Good: Result or a custom class for operations that can fail with details
interface KaCompilerFacility {
    fun compile(file: KtFile): KaCompilationResult
}
```

Throwing exceptions is appropriate for:

- **Internal compiler consistency problems** when the Analysis API can do nothing, and continuing would corrupt the state
- **Invalid API usage** such as malformed parameters

```kotlin
// Good: Exception on API misuse
val KtFunction.symbol: KaFunctionSymbol
    get() = withValidityCheck {
        check(this.isValid)
        ...
    }
```

#### Review Parameter Names Carefully

Parameter names cannot be changed without breaking source compatibility:

```kotlin
// If you ship this
fun findClass(className: Name): KaClassSymbol?

// You won't be able to change to this without breaking clients
fun findClass(name: Name): KaClassSymbol?  // Breaks named parameters!
```

#### Avoid Default Parameters in Stable APIs

Default parameters break binary compatibility when changed. Use overloads instead:

```kotlin
// Bad: Adding/changing defaults breaks binary compatibility
fun findFunctions(name: Name, scope: KaScope? = null): Sequence<KaFunctionSymbol>

// Good: Explicit overloads
fun findFunctions(name: Name): Sequence<KaFunctionSymbol>
fun findFunctions(name: Name, scope: KaScope): Sequence<KaFunctionSymbol>
```

While `@JvmOverloads` may look like a solution, it still generates the `$default` function, compatibility of which breaks on signature 
changes. E.g., for the following function:

```kotlin
@JvmOverloads 
fun test(i: Int = 0, s: String = "") {}
```

The following set of JVM methods is generated:

```java
public final class FacadeKt {
    public static final void test(int, java.lang.String);
    public static void test$default(int, java.lang.String, int, java.lang.Object);
    public static final void test(int);
    public static final void test();
}
```

Now, if the `test()` function gets a new parameter, the old `$default` will disappear. 

#### Ensure Consistent Behavior Across Overloads

Overloaded or similar methods should behave predictably:

```kotlin
interface KaSymbolProvider {
    // Bad: No common patterns
    fun findClass(classId: ClassId): KaClassSymbol?
    fun findTypeAlias(fqName: FqName): Result<KaTypeAliasSymbol>
    fun findFunction(classId: ClassId?, name: Name): KaFunctionSymbol?

    // Good: All "find" methods return 'null' for not found
    fun findClass(classId: ClassId): KaClassSymbol?
    fun findTypeAlias(classId: ClassId): KaTypeAliasSymbol?
    fun findFunction(callableId: CallableId): KaFunctionSymbol?
}
```

#### Use Overloaded Extensions

Reduce cognitive load by providing a single entry point with specialized overloads:

```kotlin
// Base extension for all declarations
val KtDeclaration.symbol: KaDeclarationSymbol

// Specialized versions with more specific return types
val KtClass.symbol: KaClassSymbol
val KtNamedFunction.symbol: KaFunctionSymbol
val KtProperty.symbol: KaPropertySymbol

// Users only need to remember ".symbol"
```

#### Choose Return Types Thoughtfully

Select return types that provide flexibility for implementations and users.
Avoid choosing the `List` unconsciously – consider also `Sequence`, `Collection` or `Iterable`.

```kotlin
// Good: Sequence allows lazy evaluation (and lazy implementation!)
val KaType.allSupertypes: Sequence<KaType>

// Document iteration capabilities
/**
 * Returns a sequence of all supertypes of the given [KaType].
 * 
 * Supertypes may be traversed lazily.
 * The sequence can be iterated multiple times.
 */
val KaType.allSupertypes: Sequence<KaType>

// For small, always-computed collections, 'List' is fine
interface KaFunctionSymbol {
    val valueParameters: List<KaValueParameterSymbol>
}
```

At the same time, avoid using `Sequence`s "just in case" – they are more heavyweight. Also, they are prone to scoping issues, as you cannot
know when the sequence will be traversed. Use them when it can make the implementation significantly more efficient.

#### Design Collection Operations as Extensions

Make APIs more fluent by extending collection types:

```kotlin
// Good: Extension on a collection (or 'Iterable' in this particular case)
val Iterable<KaType>.commonSupertype: KaType?
get() = computeCommonSupertype(this)

// Usage is clear with collections
val supertype = listOf(stringType, intType).commonSupertype

// When collection literals come to Kotlin, this will look even nicer
val supertype = [stringType, intType].commonSupertype

// Less idiomatic: Standalone function
fun commonSupertype(types: List<KaType>): KaType?
```

#### Provide Helper Extensions on Wider Types

Provide nullable helpers for members of subtypes that are used extremely often:

```kotlin
// Calling a popular 'symbol' property requires an explicit cast
interface KaType : KaLifetimeOwner, KaAnnotated
interface KaClassType : KaType {
    val symbol: KaClassLikeSymbol
}

// Now users can easily get a symbol when it's available
val KaType.symbol: KaClassLikeSymbol?
get() = when (this) {
    is KaClassType -> symbol
    else -> null
}
```

This approach is particularly valuable when you expect users to mostly work with base types.
Avoid implementing helpers as default interface methods – those are still virtual and can be overridden by clients.

#### Use Operators Judiciously

Reserve operators for their conventional meanings:

```kotlin
// Good: Conventional operator usage
interface KaScope {
    operator fun contains(name: Name): Boolean
    operator fun get(name: Name): KaSymbol?
}

// Avoid: Unconventional operators outside DSLs
operator fun KaType.plus(other: KaType): KaType  // What does this mean?
```

#### Only Use Infix Functions in DSLs

Infix functions are rarely used in ordinary Kotlin code. Although the syntax might feel tempting, reserve it for DSLs:

```kotlin
// Bad: Nice-looking in theory, awkward in practice
infix fun KaType.isSubtypeOf(supertype: KaType)
myType isSubtypeOf serializableType

// Good: Keep it simple
fun KaType.isSubtypeOf(supertype: KaType)
myType.isSubtypeOf(serializableType)
```

#### Consider Contracts for Smart Casts

Help the compiler with smart casts using contracts:

```kotlin
inline fun KaSession.buildSubstitutor(build: KaSubstitutorBuilder.() -> Unit): KaSubstitutor {
    contract {
        callsInPlace(build, InvocationKind.EXACTLY_ONCE)
    }
    return createSubstitutor(KaSubstitutorBuilder(token).apply(build).mappings)
}
```

#### Handle File-Level Declarations Properly

When adding top-level functions or properties, consider the JVM facade name:

```kotlin
// File: KaTypeUtils.kt
@file:JvmName("KaTypes")  // Better than "KaTypeUtilsKt"

fun KaType.isSubtypeOf(other: KaType): Boolean
val KaType.isNullable: Boolean
```

#### Avoid `Object.clone()`

The `clone()` method is fundamentally broken in Java. It performs shallow copying, bypasses constructors, and requires manual casting.
Offer explicit copying methods instead:

```kotlin
// Bad: Using clone()
interface KaAnnotation : Cloneable

// Good: Explicit copy with clear semantics
interface KaAnnotation {
    /**
     * Creates a deep copy of this annotation with all its values.
     * The copy is independent and can be safely modified.
     */
    fun copy(): KaAnnotation
}
```

### Mind the Inlines

> [!TIP]
> Inline functions and constants are copied into client code at compile time.
> Changes to their implementation won't affect existing compiled code.

#### Understand Inlining Implications

When you use `const val` or `inline fun`, the implementation is copied directly into client bytecode. This means:

```kotlin
// If clients compile against version 1.0
const val DEFAULT_TIMEOUT = 1000

inline fun performOperation(action: () -> Unit, timeout: Int = DEFAULT_TIMEOUT) {
    ...
}

// Changing DEFAULT_TIMEOUT to 2000 in version 1.1 won't affect already-compiled clients!
// They will continue using 1000 until recompiled
```

#### Think Twice Before Using Inline Functions

Inline functions have limited evolution possibilities and expose implementation details:

```kotlin
// Bad: Inline function limits future evolution
inline fun <reified T : KaSymbol> KaModule.topLevelSymbolsOfType(): Sequence<T>

// Good: Regular function allows internal changes
fun <T : KaSymbol> KaModule.topLevelSymbolsOfType(klass: KClass<T>): Sequence<T>

// Convenience: You can still provide an inline function with a trivial, delegating body
inline fun <reified T : KaSymbol> KaModule.topLevelSymbolsOfType(): Sequence<T> =
    topLevelSymbolsOfType(T::class)

// Even better: let the users filter symbols by themselves 
// or provide distinct functions for specific symbols
val KaModule.topLevelSymbols: Sequence<KaSymbol>
```

#### Design Extensible Inline Functions

For critical inline functions like `analyze`, embed hooks for future functionality:

```kotlin
inline fun <R> analyze(
    element: PsiElement,
    action: KaSession.() -> R
): R {
    // Current implementation
    val session = createSession(element)

    // Hook for future additions without breaking compatibility
    beforeAnalysis?.invoke(session)

    return try {
        session.action()
    } finally {
        // Future cleanup hook
        afterAnalysis?.invoke(session)
    }
}
```

Be prepared for suspension! Unless the lambda parameter of an inline function is marked with `crossinline`, or a lambda runs inside a
synchronized block, a suspend call may happen inside the passed lambda, and your function body may never be fully executed.

### Component Architecture

Components group related functionality, providing APIs through the `KaSession`:

```kotlin
interface KaSession : KaLifetimeOwner,
    KaResolver,
    KaSymbolRelationProvider,
    KaDiagnosticProvider,
    KaScopeProvider,
    ...
```

#### Check Existing Components Before Creating New Ones

Before adding a new component, verify that your functionality doesn't fit within an existing one:

```kotlin
// Before creating KaInheritanceComponent, check if KaTypeProvider already handles similar functionality
interface KaTypeProvider : KaSessionComponent {
    val KaType.directSupertypes: Sequence<KaType>  // Type inheritance is part of types
    val KaType.allSupertypes: Sequence<KaType>
}
```

#### Provide Context Parameter Bridges

For every component function and property, provide a bridge that accepts a session as a context parameter:

```kotlin
interface KaScopeProvider : KaSessionComponent {
    val KaDeclarationContainerSymbol.memberScope: KaScope
}

@KaContextParameterApi
context(s: KaSession)
val KaDeclarationContainerSymbol.memberScope: KaScope
    get() = with(s) { memberScope }
```

Usually they should be auto-generated by `Analysis API Public API Checks (overwrite binary output)` run configuration.
In exceptional cases (like top-level inline function), you may manually add them with the `@KaCustomContextParameterBridge` annotation.

### Documentation Standards

> [!TIP]
> Write clear, comprehensive KDocs covering happy paths, edge cases, also providing examples.
> Mention contracts, exceptions, and nullability behavior.

Your documentation should answer:

- What is the endpoint's purpose?
- What are the use-cases?
- What are the pre-conditions and post-conditions?
- What does it normally return?
    - If it returns a `Collection` or similar, is the item order specified (or at least stable)?
    - If it returns a `Sequence`, can the sequence be iterated over more than once?
    - In which cases does it return `null` or empty collections?
- When does it throw exceptions?
- What happens with invalid or edge-case inputs?

When including code examples in documentation, ensure they demonstrate real-world scenarios that help users understand practical usage.
Examples should be copy-paste ready, correctly handle all necessary cases, and follow proper Kotlin code style.

Use meaningful names that reflect the domain or are just more pleasant to see, rather than generic placeholders.
Avoid random variable names like `a`, `x`, or `temp`.
The often-used `Foo` placeholder may be acceptable when discussing declarations in general terms.

```kotlin
/**
 * The function symbol for the original Java getter method.
 *
 * #### Example:
 *
 * `​`​`
 * public class JavaClass {
 *     private int field;
 *
 *     public int getField() {
 *         return field;
 *     }
 * }
 * `​`​`
 *
 * In the synthetic property for `field`, [javaGetterSymbol] is the function symbol for `getField`.
 */
val javaGetterSymbol: KaNamedFunctionSymbol
```

### Structure documentation comments

KDoc is not just a custom comment syntax – it supports [Markdown](https://daringfireball.net/projects/markdown/syntax) tags.
Inside longer documentation comments, use Markdown headers (`###`) for individual comment sections.

```kotlin
/**
 * The abbreviated type for this expanded [KaType], or `null` if this type has not been expanded from an abbreviated type or the
 * abbreviated type cannot be resolved. [...]
 *
 * ### Resolvability
 *
 * Even when this [KaType] is an expansion, the abbreviated type may be `null` if it is not resolvable from this type's use-site module.
 * This can occur when the abbreviated type from a module `M1` was expanded at some declaration `D` in module `M2`, and the use-site
 * module uses `D`, but only has a dependency on `M2`. Then the type alias of `M1` remains unresolved and [abbreviation] is `null`.
 *
 * ### Type arguments and nested abbreviated types
 *
 * The type arguments of an abbreviated type are not converted to abbreviated types automatically. That is, if a type argument is a type
 * expansion, its [abbreviation] doesn't automatically replace the expanded type. [...]
 *
 * ### Transitive expansion
 *
 * Types are always expanded to their final form. That is, if we have a chain of type alias expansions, the [KaType] only represents the
 * final expanded type, and its [abbreviation] the initial type alias application. [...}]
 */
val abbreviation: KaUsualClassType?
```

Use the documentation viewer in IntelliJ IDEA (`F1` by default) to check how your documentation looks like for the users.

### Document properties and functions differently

As explained in the [Choose between properties and functions based on semantics](#choose-between-properties-and-functions-based-on-semantics)
section, function and property endpoints are different. The difference should be reflected in the documentation.

For functions, explain what the action does:

```kotlin
/**
 * Creates a new [KaType] based on the given type with the updated nullability specified by [isMarkedNullable].
 * @retur
 */
fun KaType.withNullability(isMarkedNullable: Boolean): KaType
```

`@param` and `@return` tags can be omitted if the user can get the same information from a more natural description at the beginning
of the KDoc. However, add them if there are special cases that need explanation, or to make the description more concise so understanding
the general goal of a function is easier.

For properties, just explain what the user gets (or sets) when accessing the property. Avoid verbs like "returns", "computes" or similar:

```kotlin
/**
 * A [KaScope] containing the top-level declarations (such as classes, functions and properties) in the given [KaFileSymbol].
 */
public val KaFileSymbol.fileScope: KaScope
```

### Test Your Documentation's Completeness

Try to "hack" your own KDoc. Look for ambiguities or missing information. Don't leave users guessing about special cases:

```kotlin
/**
 * The compile-time constant initializer for the given property, if available.
 */
val KaPropertySymbol.compileTimeInitializer: KaConstantValue?

// Questions that should be answered:
// - Is the expression evaluated or just parsed?
// - What happens if the property is not `const`?
// - What happens if the initializer isn't a compile-time constant, but it still can be evaluated?
// - Does the property work for non-Kotlin (e.g., Java) declarations, including `KaSyntheticJavaPropertySymbol`?
```

#### Always Proofread Documentation

Before committing:

1. Run documentation through a grammar checker (Grammarly, LanguageTool, etc.)
2. Have someone else review for clarity
3. Verify examples compile and work correctly

#### Update External Documentation

After adding or modifying APIs:

1. Update the documentation website at https://github.com/Kotlin/analysis-api
2. Update migration guides if deprecating existing APIs

#### Mark K1-Specific Limitations

If your API isn't implemented for K1, make it clear:

```kotlin
/**
 * A list of [KaContextParameterSymbol]s directly declared in the callable symbol.
 *
 * As context parameters are not supported in the K1 Kotlin compiler,
 * in the K1 API implementation the resulting list is always empty.
 */
@KaK1Unsupported
val KaCallableSymbol.contextParameters: List<KaContextParameterSymbol>
```

Also, if the behavior differs between K1 and K2, describe those differences:

```kotlin
/**
 * Compiles the given [file] in-memory (without dumping the compiled binaries to the disk).
 * The file might be either a Kotlin source file, or a [KtCodeFragment].
 * 
 * The function rethrows exceptions from the compiler, wrapped in [KaCodeCompilationException]. The implementation should wrap the
 * `compile()` call into a `try`/`catch` block when necessary.
 * 
 * ### K1 Implementation Limitations
 * 
 * The K1 implementation of [compile] does not support [KtCodeFragment]s.
 * The only existing use-case for code fragment compilation is code evaluation in the JVM debugger, and in the K1 Kotlin plugin for
 * IntelliJ IDEA compilation is implemented on the IDE side.
 */
fun compile(file: KtFile, configuration: CompilerConfiguration, target: KaCompilerTarget): KaCompilationResult
```

Always refer to the K1 compiler and the K1 implementation simply as "K1".
Avoid adjectives such as "legacy", "classic", "old" or similar.

## Naming Conventions

### Use Common Prefixes for Top-Level Class-Like Declarations

The Analysis API uses the `Ka` prefix, while the PSI API uses `Kt` or `KDoc` prefixes.

### Use Common Property and Parameter Names

Entity naming in the Analysis API differs on whether an entity is referred from inside the same entity hierarchy or from another place.
Such as, accessing a getter symbol of a `KaPropertySymbol` is different from accessing a `KaPropertySymbol` corresponding to a `KtProperty`.

For cross-references inside a single entity hierarchy, prefer simpler names to avoid verbosity:

```kotlin
interface KaTypeParameterOwnerSymbol : KaSymbol {
    val typeParameters: List<KaTypeParameterSymbol>  // Not 'typeParameterSymbols'
}
```

Across domain boundaries, prefer more descriptive names:

- `KaSymbol`s
    - `symbol` (not `declaration`!) for an arbitrary symbol and when no disambiguation is needed (almost always)
    - `functionSymbol` for a `KaFunctionSymbol` and its subtypes
    - `propertySymbol` for a `KaPropertySymbol` and its subtypes
    - `classSymbol` for a `KaClassSymbol`
    - `fileSymbol` for a `KaFileSymbol`
    - etc.
- `KaType`s
    - `type` for an arbitrary type
    - `classType`, `functionType` and others when referring specifically to a function, class, or type (rarely needed)
- `PsiElement`s
    - `psi` for a `PsiElement`, or for a `KtElement`
    - `declaration` for a `KtDeclaration` or its subtypes (when no disambiguation is needed)
    - `expression` for a `KtExpression` or its subtypes
        - `callExpression` (not `call`!) for `KtCallExpression`
    - `typeReference` (not `type`!) for `KtTypeReference`
    - `script` for a `KtScript`

Rule of thumb: is it clear for the user that they get an entity of a different API domain?

- PSI ↔ Analysis API
- `KaSymbol` ↔ `KaType`

For example:

```kotlin
sealed class KaClassType : KaType {
    // From `KaType` to `KaSymbol`
    val symbol: KaClassLikeSymbol
}

// From `KaSymbol` to `KaTye`
val KaClassifierSymbol.defaultType: KaType
```

### Avoid Unnecessary `ka` and `kt` Prefixes

Don't use `ka` and `kt` prefixes for callable names unless absolutely necessary to resolve conflicts:

```kotlin
// Good: Clean names
val type: KaType
val annotations: List<KaAnnotation>
val declaration: KtDeclaration

// Bad: Unnecessary prefixes
val kaType: KaType
val kaAnnotations: List<KaAnnotation>
val ktDeclaration: KtDeclaration

// Might work, but think of alternatives (e.g., `KaModuleProvider.find(element)`)
fun PsiElement.findKaModule(): KaModule?
```

However, if you handle both API layers in a single place (e.g., you map the Kotlin PSI to FIR), prefer adding prefixes at least for
non-primary entities:

```kotlin
fun findDeclaration(file: FirFile, ktDeclaration: KtDeclaration): FirDeclaration
```

### Check for Potential Name Clashes

Check for both syntactic and semantic naming conflicts within the API and with external libraries.
Before introducing new names, verify they don't clash with existing functionality or create confusion:

```kotlin
// Bad: Clash with 'kotlin.Array'
sealed class KaAnnotationValue {
    class Array : KaAnnotationValue()
}

// Good: A distinct name
sealed class KaAnnotationValue {
    class ArrayValue : KaAnnotationValue()
}
```

### Avoid Getter-Like Functions

Avoid `get` prefixes unless they carry special meaning. Choose verbs that reflect the operation's nature.
Function names should clearly indicate their behavior and return characteristics:

```kotlin
// Bad: Unnecessary 'get' prefix
fun getSupertypes(): List<KaType>
fun KtElement.getDiagnostics(filter: KaDiagnosticCheckerFilter): List<KaDiagnosticWithPsi<*>>
fun getClass(classId: ClassId): KaClassSymbol?

// Good: Direct naming
val supertypes: List<KaType>
fun KtElement.diagnostics(filter: KaDiagnosticCheckerFilter): List<KaDiagnosticWithPsi<*>>
fun findClass(classId: ClassId): KaClassSymbol?  // 'find' indicates nullable result

// Good: Use appropriate verbs for actions
fun buildClassType(classId: ClassId): KaType
fun KaType.render(): String
fun KtExpression.evaluate(): KaConstantValue?
```

### Avoid Redundant Naming

Don't repeat type information or parameter details in names unless ambiguity takes place. Keep names concise and meaningful.

```kotlin
// Bad: Redundant type information
val annotationList: List<KaAnnotation>
fun findClassByClassId(classId: ClassId)

// Good: Clean, descriptive names
val annotations: List<KaAnnotation>
fun findClass(classId: ClassId)
```

### Choose Precise Boolean Property Prefixes

Use prefixes that clearly convey the intent and distinguish between different boolean concepts.
Boolean property names should indicate what they're checking and when the check is performed:

```kotlin
// Bad: Ambiguous meaning
val nullable: Boolean
val overridden: Boolean
val inline: Boolean

// Good: Clear intent with appropriate prefixes
val isNullable: Boolean          // Current state
val hasTypeParameters: Boolean   // Existence check
val canBeOverridden: Boolean     // Capability check
val shouldBeInlined: Boolean     // Recommendation

// Good: Distinguish between temporal states
val shouldBeEvaluated: Boolean   // Future action recommended
val willBeEvaluated: Boolean     // Future action will happen
val isEvaluated: Boolean         // Past/current state
```

Check that the names of flags are grammatically correct, and they mean exactly what they say.

Mind a difference between an actor and an object. E.g., `shouldResolve` is a property of the resolver itself, while for a declaration the
correct flag name is `shouldBeResolved`.

### Name Sealed Classes Appropriately

Use simple names for nested sealed class implementations, also dropping the `Ka` prefix.
Reserve complex names for top-level classes.

```kotlin
// Good: Simple names for nested implementations
sealed class KaConstantValue {
    class StringValue : KaConstantValue()  // Without 'Value', will clash with 'kotlin.String'
    class IntValue : KaConstantValue()
    class CharValue : KaConstantValue()
}

// Good: Descriptive names for top-level sealed implementations
sealed class KaClassifierSymbol : KaDeclarationSymbol()
sealed class KaClassSymbol : KaClassifierSymbol()
abstract class KaTypeAliasSymbol : KaClassifierSymbol()
```

### Use Consistent Terminology

Use established single-word forms for compound terms commonly used in the programming language industry.

- `supertype` and `subtype` (not `superType` or `subType`)
- `builtin` (not `builtIn`)
- `uint` (not `uInt`, but `UInt` when in PascalCase)

## Guard API Endpoints with Annotations

The Analysis API serves various projects with different stability requirements.
To prevent accidental usage of unstable APIs or usage-tailored functionality, the Analysis API uses a set of opt-in annotations.

Note that the binary compatibility checker doesn't propagate opt-in annotations from outer classes.
If the outer class is annotated, add the same opt-in annotation to all nested classes as well.

### Experimental API Markers

- `@KaExperimentalApi`
    - Marks user-facing APIs that are experimental.
      These APIs are intended for public consumption but may change or be removed without deprecation cycles.

### API Layer Markers

- `@KaPlatformInterface`
    - Marks APIs intended for Analysis API implementations and platforms.
      These APIs define contracts between the core API and platform implementations, are neither stable nor intended for end-user
      consumption.

### Internal API Markers

These APIs are intended to only be used in a specific project, or a group of projects.

- `@KaNonPublicApi`
    - Marks APIs used within JetBrains projects but not intended for external consumption.
      These APIs have relaxed compatibility guarantees and may change without deprecation cycles.
- `@KaIdeApi`
    - Marks APIs designed for the Kotlin IntelliJ plugin.
      These APIs are implemented in the Analysis API for efficiency reasons but are too specific for general use and have no compatibility
      guarantees outside the IDE context.
- `@KaImplementationDetail`
    - Marks APIs that are implementation details of the Analysis API itself.
      These APIs exist on the public surface due to architectural constraints but should never be used outside Analysis API implementation
      modules.

### Other Markers

- `@KaK1Unsupported`
    - Marks APIs that are only supported in the K2 implementation of the Analysis API.
      See the [Mark K1-Specific Limitations](#mark-k1-specific-limitations) for more details.

### Unstable Dependency Markers

These APIs are intended for all users but depend on unstable language features.

- `@KaContextParameterApi`
    - Marks APIs that use context parameters, an experimental Kotlin language feature.

### Adding New Annotations

When existing annotations don't cover your use case, consider adding new opt-in annotations.

1. **Identify the specific audience**  
   Who should use this API and under what constraints?
2. **Define stability guarantees**  
   What compatibility promises can you give? How are they unique?
3. **Identify the scope**. 
   If the annotation is only applicable to a very narrow number of cases, using a more generic one (e.g., `@KaExperimentalApi`) with a
   dedicated comment is preferred.
4. **Write comprehensive documentation**  
   Explain the purpose, intended audience, and compatibility guarantees

## Experimental API

Experimental APIs have relaxed evolution rules, but they still should maintain quality standards.
Write proper documentation and cover the endpoints with tests. When applicable, explain why the API is kept experimental.

Remember – an experimental API becomes stable someday. Avoid breaking your first clients without a strong reason.

All experimental API must be marked with the `@KaExperimentalApi` annotation.

For detailed information about the lifecycle of experimental APIs and their transition to stable status, see
the [API Evolution Guide](api-evolution.md).

## API Implementation

> [!TIP]
> API Implementations don't need to provide API compatibility guarantees. However, you should prevent their accidental usage outside
> the implementation module where possible. Use the fail-fast approach – check input values and throw consistency exceptions when necessary.

### Use `internal` by Default

Implementation declarations should be internal unless they need to be part of the public API surface (like `KaFirDiagnostic`).
Minimize accidental exposure of implementation details.

If you cannot make the declaration internal, mark it with `@KaImplementationDetail`.

### Lifetime Management

All API implementations must validate lifetime ownership and fail fast on invalid access.

Lifetime safety is critical for preventing memory leaks and ensuring correctness:

```kotlin
internal class KaFirNamedClassSymbol(
    override val firSymbol: FirClassSymbol,
    override val analysisSession: KaFirSession
) : KaNamedClassSymbol {
    override val name: Name
        // Either use 'withValidityAssertion' (preferred) or 'assertIsValidAndAccessible()'
        get() = withValidityAssertion {
            firSymbol.name
        }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion {
            firSymbol.typeParameters.map { it.toKaSymbol() }
        }
}
```

For endpoints consuming `PsiElement`s, use `withPsiValidityAssertion()` instead:

```kotlin
override val KtFile.symbol: KaFileSymbol
    get() = withPsiValidityAssertion {
        KaFirFileSymbol(this, analysisSession)
    }
```

Remember to guard properties you declare in the primary constructor: 

```kotlin
class KaScopeWithKindImpl(
    private val backingScope: KaScope,
    private val backingKind: KaScopeKind,
) : KaScopeWithKind {
    override val scope: KaScope get() = withValidityAssertion { backingScope }
    override val kind: KaScopeKind get() = withValidityAssertion { backingKind }
}
``` 

For `equals()`, `hashCode()` and `toString()` implementations, avoid validation as these methods are often used in uncontrolled contexts.

### Handle Errors Appropriately

Fail fast with clear messages, use appropriate exception types, and document error conditions.

Robust error handling improves developer experience. Use `checkWithAttachment`, `requireWithAttachment`, and `errorWithAttachment`, passing
the relevant information. Never pass sensitive information such as code snippets, or names from source code (e.g., names of individual
classes or functions) in the exception message directly.

```kotlin
private fun calculateLazyBodyForResultProperty(firProperty: FirProperty, designation: FirDesignation) {
    val newInitializer = revive<FirAnonymousInitializer>(designation)
    val body = newInitializer.body
    requireWithAttachment(body != null, { "${FirAnonymousInitializer::class.simpleName} without body" }) {
        withFirDesignationEntry("designation", designation)
        withFirEntry("initializer", newInitializer)
    }

    ...
}
```

Document throwing behavior:

```kotlin
/**
 * @throws IllegalArgumentException if [name] is a special name like <init>
 */
fun findFunctions(name: Name): Sequence<KaFunctionSymbol> {
    require(!name.isSpecial) { "Special names are not supported: $name" }
    // ...
}
```

## Further Reading

- [Analysis API Documentation](https://github.com/Kotlin/analysis-api)
- [Kotlin Evolution and Enhancement Process (KEEP)](https://github.com/Kotlin/KEEP)
- [JVM Binary Compatibility Specification](https://docs.oracle.com/javase/specs/jls/se24/html/jls-13.html)
    - [API design practices for Java from IBM](https://developer.ibm.com/articles/api-design-practices-for-java/)
- [Kotlin API Design Guidelines](https://kotlinlang.org/docs/api-guidelines-introduction.html)