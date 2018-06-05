# Diagnostics 테스트 형식 specification

각각의 diagnostics 테스트는 하나 또는 여러 개의 Kotlin 혹은 Java 소스 파일들의 코드를 포함하는 하나의 .kt 파일로 이루어집니다.
각각의 diagnostic 에 대한 경고나 에러는 다음과 같은 방식으로 표기됩니다:

    <!DIAGNOSTIC_FACTORY_NAME!>element<!>

여기서 `DIAGNOSTIC_FACTORY_NAME`은, 주로 `Errors*` 클래스 중에 한 가지에 있는 상수들의 하나인, diagnostic의 이름입니다.

Diagnostic 존재 여부 뿐만 아니라 어떤 arguments가 유저에게 render 될 지를 테스트 하기 위해서, 그들 전부를 diagnostic 이름 다음에 `;`로 구분하여 string 표기를 하고, 이들을 괄호에 넣어서 표기해야 합니다:

    return <!TYPE_MISMATCH(String; Nothing)!>"OK"<!>

참고: 인자로 어떤 텍스트를 넣어야 할 지 모르겠다면, 단순히 괄호를 비웁니다. 그렇게 하면 실패한 테스트가 assertion 메시지를 통해 실제 값을 보여줄 것입니다.

## Directives

Several directives can be added to the beginning of a test file with the following syntax:

    // !DIRECTIVE

### 1. DIAGNOSTICS

This directive allows to exclude some irrelevant diagnostics (e.g. unused parameter) from a certain test, or to test only a specific set of diagnostics.

The syntax is

    '([ + - ! ] DIAGNOSTIC_FACTORY_NAME | ERROR | WARNING | INFO ) +'

  where

* `+` means 'include';
* `-` means 'exclude';
* `!` means 'exclude everything but this'.

  Directives are applied in the order of appearance, i.e. `!FOO +BAR` means include only `FOO` and `BAR`.

#### Usage:

    // !DIAGNOSTICS: -WARNING +CAST_NEVER_SUCCEEDS

    // !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE


### 2. CHECK_TYPE

The directive adds the following declarations to the file:

    fun <T> checkSubtype(t: T) = t

    class Inv<T>
    fun <E> Inv<E>._() {}
    infix fun <T> T.checkType(f: Inv<T>.() -> Unit) {}

With that, an exact type of an expression can be checked in the following way:

    fun test(expr: A) {
       expr checkType { _<A>() }
    }

`CHECK_TYPE` directive also disables `UNDERSCORE_USAGE_WITHOUT_BACKTICKS` diagnostics output.

#### Usage:

    // !CHECK_TYPE

### 3. FILE

The directive lets you compose a test consisting of several files in one actual file.

#### Usage:

    // FILE: A.java
    /* Java code */

    // FILE: B.kt
    /* Kotlin code */

### 4. LANGUAGE

This directive lets you enable or disable certain language features. Language features are named as enum entries of the class `LanguageFeature`.
Each feature can be enabled with `+`, disabled with `-`, or enabled with warning with `warn:`.

#### Usage:

    // !LANGUAGE: -TopLevelSealedInheritance

    // !LANGUAGE: +TypeAliases -LocalDelegatedProperties

    // !LANGUAGE: warn:Coroutines

### 5. API_VERSION

This directive emulates the behavior of the `-api-version` command line option, disallowing to use declarations annotated with `@SinceKotlin(X)` where X is greater than the specified API version.
Note that if this directive is present, the NEWER_VERSION_IN_SINCE_KOTLIN diagnostic is automatically disabled, _unless_ the "!DIAGNOSTICS" directive is present.

#### Usage:

    // !API_VERSION: 1.0
