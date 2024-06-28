# Format specification for diagnostic tests

Each diagnostic test consists of a single `.kt` file containing the code of one or several Kotlin or Java source files.
Each diagnostic, be it a warning or an error, is marked in the following way:

    <!DIAGNOSTIC_FACTORY_NAME!>element<!>

where `DIAGNOSTIC_FACTORY_NAME` is the name of the diagnostic which is either:

* a constant from one of the `Errors*`/`Fir*Errors` classes;
* a debug diagnostic implemented specifically for the test infrastructure (e.g. `DEBUG_INFO_EXPRESSION_TYPE`).

To test not only the presence of the diagnostic but also the arguments which will be rendered to the user, provide in parentheses after the diagnostic name a string representation of all of them delimited with `;`:

    return <!TYPE_MISMATCH("String; Nothing")!>"OK"<!>

If you're unsure what text should be added for the parameters, just leave the string representation empty:

    return <!TYPE_MISMATCH("")!>"OK"<!>

and the failed test will present the actual values in the assertion message.

# Directives

Read more about test directives [here](../../test-infrastructure/ReadMe.md#directives).

Below is the list of some (but not all) directives supported by the test infrastructure.

### FILE & MODULE

Read more about the `FILE` and `MODULE` directives [here](../../test-infrastructure/ReadMe.md#module-structure-directives).

### LANGUAGE

This directive allows you to enable or disable certain language features.
Language features are named as entries of [the enum class `LanguageFeature`](../../util/src/org/jetbrains/kotlin/config/LanguageVersionSettings.kt).
Each language feature can be enabled with `+`, disabled with `-`, or enabled with a warning with `warn:`.

#### Usage:

    // LANGUAGE: -TopLevelSealedInheritance

    // LANGUAGE: +TypeAliases -LocalDelegatedProperties

    // LANGUAGE: warn:Coroutines

### DIAGNOSTICS

This directive allows you to exclude some irrelevant diagnostics (e.g. `UNUSED_PARAMETER`) from a certain test or to include others.

The syntax is:

    '([ + - ] DIAGNOSTIC_FACTORY_NAME | ERROR | WARNING | INFO ) +'

where:

* `+` means 'include';
* `-` means 'exclude'.

Diagnostics are included or excluded in the order of appearance (e.g. `+FOO -BAR` means "include `FOO` but not `BAR`").

#### Usage:

    // DIAGNOSTICS: -WARNING +CAST_NEVER_SUCCEEDS

    // DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

### CHECK_TYPE

This directive adds the following declarations to the file:

    fun <T> checkSubtype(t: T) = t
    
    class CheckTypeInv<T>
    fun <E> CheckTypeInv<E>._() {}

    infix fun <T> T.checkType(f: CheckTypeInv<T>.() -> Unit) {}

These declarations allow you to check an exact type of an expression in the following way:

    fun test(expr: A) {
       expr checkType { _<A>() }
    }

In diagnostic tests, `CHECK_TYPE` directive also disables diagnostics related to usages of `_` as a name.

#### Usage:

    // CHECK_TYPE

### CHECK_TYPE_WITH_EXACT

This directive adds the following declarations to the file:

    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    fun <T> checkExactType(expr: @kotlin.internal.Exact T) {}

    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    fun <T> checkTypeEquality(reference: @kotlin.internal.Exact T, expr: @kotlin.internal.Exact T) {}

Like the `CHECK_TYPE` directive, these declarations allow you to check an exact type of an expression:

    fun test(expr: A) {
        checkExactType<A>(expr)
        checkTypeEquality(A(), expr)
    }

Unlike the `CHECK_TYPE` directive, these declarations:

* can be used in e.g. codegen tests (as codegen tests don't disable diagnostics related to usages of `_` as a name);
* don't require you to explicitly specify the type if you have a reference expression of this type (which is useful when checking for non-denotable types).

#### Usage:

    // CHECK_TYPE_WITH_EXACT

### API_VERSION

This directive emulates the behavior of the `-api-version` command-line option, disallowing to use declarations annotated with `@SinceKotlin(X)` where `X` is greater than the specified API version.
Note that if this directive is present, the `NEWER_VERSION_IN_SINCE_KOTLIN` diagnostic is automatically disabled, _unless_ the corresponding `DIAGNOSTICS` directive is present.

#### Usage:

    // API_VERSION: 1.0

### RENDER_DIAGNOSTICS_MESSAGES

This K2-specific directive forces the test infrastructure to print diagnostic arguments for *all* diagnostics.

#### Usage:

    // RENDER_DIAGNOSTICS_MESSAGES
