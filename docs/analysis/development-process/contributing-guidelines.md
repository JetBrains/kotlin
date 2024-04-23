# Contribution Guidelines

This document is a contribution guideline for the development of **Analysis API** project. Please, follow it for your changes to be
accepted.

# General Guidelines

* Follow the [Code Style](code-style.md)
* Write new code in **Kotlin**. It is totally okay to write Java code in existing Java files. You can always use **Java To Kotlin
  Converter** to convert it to Kotlin.
* Always write [Unit Tests](#always-write-unit-tests-for-your-code) for your changes.
* Always perform **code formatting** and call **import optimizer** before committing your changes.
* Do not use short names in declarations names
    * Bad: `val dclSmbl: KtDeclarationSymbol`
    * Good `val declarationSymbol: KtDeclarationSymbol`
* [Always add KDoc](#always-add-kdoc-to-any-public-declaration-inside-analysis-apianalysisanalysis-api-module) to any public declaration inside `analysis-api` module.
  * A good KDoc should include
    * What the declaration purpose and how it should be used?
    * Explanation of corner cases
    * Examples
* Keep Analysis API Surface Area [Concise and Minimal](#keep-analysis-api-surface-area-concise-and-minimal)
* Follow Analysis API [Implementation Contracts](#follow-analysis-api-implementation-contracts)
* Keep your classes and functions with [the lowest possible visibility](#keep-your-classes-and-functions-with-the-lowest-possible-visibility)
* Properly design [utility functions](#properly-design-utility-functions) & do not overuse them

# Guidelines in more details

## Always add KDoc to any public declaration inside [analysis-api](../../../analysis/analysis-api) module

This includes:

* Public classes;
* Public class members (functions/properties);
* Class constructor parameters which are declared as public class members via `val`/`var`;
* Top-level functions/properties.

Do not hesitate to make the KDoc as long and **detailed** as possible until the information you write will help others to use your API
changes in a better way.

### A good KDoc should include

#### What the declaration purpose and how it should be used?

Please do not include obvious KDocs, which do not add additional information. Always describe the purpose of the declaration.

Bad (adds no information to the declaration name):

```kotlin
public class KtAnnotationApplication(
    /**
     * ClassId of an annotation
     */
    val classId: ClassId
)
```

Good:

```kotlin
public class KtAnnotationApplication(
    /**
     * A fully qualified name of an annotation class which is being applied
     */
    val classId: ClassId
)
```

#### Explanation of corner cases and examples

Please, explain (better with examples) how the new functionality you add behaves on corner cases.

Example:

```kotlin
/**
 * Get type of given expression.
 *
 * Return:
 * - [KtExpression] type if given [KtExpression] is real expression;
 * - `null` for [KtExpression] inside packages and import declarations;
 * - `Unit` type for statements;
 */
public fun KtExpression.getKtType(): KtType?
```

## Keep Analysis API Surface Area Concise and Minimal

As you already know, Analysis API is a compiler API used to retrieve compiler-related information for FIR IDE Plugin. It is used in a wide
range of features: code completion, debugger, inspections, and all other features which require compiler-related information. So it is
important to keep the Analysis API Surface Area concise. Whether you want to introduce a new method or change the behavior of the existing
method, consider

* Will your change be useful for others?
* Will it be possible to implement your task without modifying Analysis API? If yes, maybe it would be a better solution to just implement
  the functionality you need as some utility function on your project side.

## Always write Unit Tests for your code

It was already mentioned in the general part of the guidelines but Unit Tests are an important thing for public API. So, write **Unit Test**
whenever you add new functionality or modify existing behavior (either fixing bug or adding feature). A good unit test:

* Should cover basic usage scenarios
* Should cover corner-cases

If you fixed a bug or added new functionality to an existing feature, consider adding test(s) which cover it.

## Follow Naming Conventions

* All public declarations which are exposed to the surface area of Analysis API should have `Kt` prefix. E.g, `KtSymbol`, `KtConstantValue`.

## Follow Analysis API Implementation Contracts

* Add `KtLifetimeOwner` as supertype for all declarations that contains other `KtLifetimeOwner` inside (e.g., via parameter types,
  function return types) to ensure that internal `KtLifetimeOwner` are not exposed via your declaration.
* You have some declaration which implements `KtLifetimeOwner`. It means that this declaration has a lifetime. And this declaration has
  to be checked to ensure that it is not used after its lifetime has come to an end. To ensure that all methods (except `hashCode`/`equals`
  /`toString`) and properties should be wrapped into `withValidityAssertion { .. }` check. For simple cases there a constructor parameter
  should be exposed, `by validityAsserted` should be used. If the constructor parameter should be reused without validity assertion,
  it should be private and the name should contain `backing` prefix.

```kotlin
public class KtCall(
    private val backingSymbol: KtSymbol,
    private val backingIsInvokeCall: Boolean,
    additionalInformation: String,
) : KtLifetimeOwner {
    public val symbol: KtSymbol get() = withValidityAssertion { backingSymbol }
    public val isInvokeCall: Boolean get() = withValidityAssertion { backingIsInvokeCall }
    public val additionalInformation: String by validityAsserted(additionalInformation)

    public fun isImplicitCall(): Boolean = withValidityAssertion {
        // IMPL
    }

    override fun equals(other: Any?): Boolean { // no withValidityAssertion
        return this === other || 
                other is KtCall && 
                other.backingSymbol == backingSymbol &&
                other.backingIsInvokeCall == backingIsInvokeCall
    }
  
    override fun hashCode(): Int { // no withValidityAssertion
        return Objects.hashCode(backingSymbol, backingIsInvokeCall)
    }
  
    override fun toString(): String { // no withValidityAssertion
        // IMPL
    }
}
```

## Keep your classes and functions with the lowest possible visibility

The only part of Analysis API which should be exposed is the Analysis API surface area (the API itself). All other declarations should be
kept `internal` or `private`. To ensure that, [analysis-api module](../../../analysis/analysis-api)
has [Library Mode](https://github.com/Kotlin/KEEP/blob/master/proposals/explicit-api-mode.md) enabled. This will enforce that only
declarations which are supposed to be exposed are really exposed. There are no guarantees on the non-surface part of analysis API on binary
and source compatibility.

Also, the implementation modules should be considered as internal themselves. Please, keep declarations there `internal` or `private` too
then it is possible. Implementation modules are:

* [Analysis API FIR Implementation](../../../analysis/analysis-api-fir)
* [Analysis API FE1.0 Implementation](../../../analysis/analysis-api-fe10)

## Properly design utility functions

In compiler-related code (and Analysis API is compiler-related 😀), there may be a lot of Kotlin top-level utility functions and properties
to work with AST, `PsiElements`, and others. Such code pollutes public and internal namespaces and introduces a lot of functions with
unclear semantics:

* If you have a utility function that uses one or more private functions and those functions are used only by it, consider encapsulating the
  whole functions family into a class or object.

Bad:

```kotlin
internal fun render(value: KtAnnotationValue): String = buildString { renderConstantValue(value) }

private fun StringBuilder.renderConstantValue(value: KtAnnotationValue) {
    when (value) {
        is KtAnnotationApplicationValue -> renderAnnotationConstantValue(value)
            ...
    }
}

private fun StringBuilder.renderConstantAnnotationValue(value: KtConstantAnnotationValue) {
    append(value.constantValue.renderAsKotlinConstant())
}

// A lot of other non-related utility functions in the same file
```

Good:

```kotlin
internal object KtAnnotationRenderer {
    fun render(value: KtAnnotationValue): String = buildString { renderConstantValue(value) }

    private fun StringBuilder.renderConstantValue(value: KtAnnotationValue) {
        when (value) {
            is KtAnnotationApplicationValue -> renderAnnotationConstantValue(value)
                ...
        }
    }

    private fun StringBuilder.renderConstantAnnotationValue(value: KtConstantAnnotationValue) {
        append(value.constantValue.renderAsKotlinConstant())
    }
}
```

* Your utility function may be useful for others or maybe very specific for your task. Please, decide if you want others to reuse your code
  or not. If not, and it is very task-specific, consider hiding it in your implementation. Otherwise, feel free to introduce it as a
  top-level function/object. For the latter case, it would be good to have a KDoc for it :)

