// LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND: JVM, JVM_IR
// ISSUE: KT-69201
// Notes: Ignore JVM backends because of `Platform declaration clash`

// The test emulates KT-68396 but with an example `f` function
// Originally, K1 somehow matches some incorrect pairs of expect/actual functions like `copyOf`
// despite the fact they are marked by `@Suppress("NO_ACTUAL_FOR_EXPECT")` annotation.
// But K2 can't handle such functions even with the suppression because those pairs aren't being added into expect-actual map.
// It can cause exceptions on backend.
// To resolve the problem, it was decided to declare a correct `actual` functions (without `out` variance on type parameter)
// and keep the existing ones with removed `actual` modifier.
// It's not possible just to remove the existing incorrect `actual` functions because it's a breaking change.
// Also, it's not possible to add `out` variance to all existing `expect` functions because JVM builtins provider is embedded
// and it can't match its function with corresponding `expect` ones.
// Introducing a new actual together with keeping existing functions cause `OVERLOAD_RESOLUTION_AMBIGUITY` when calling `f` function in platform source-set
// To fix that issue, it was decided to mark newly added `actual` by `@Deprecated` annotation with `HIDDEN` level and fix resolving.

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

expect fun <T> Array<T>.f(): String

fun <T> Array<T>.g(): String = f()

// MODULE: platform()()(common)
// FILE: platform.kt

@Deprecated(
    "Make it matchable with expect `f` function (K2) but decrease priority to avoid `OVERLOAD_RESOLUTION_AMBIGUITY` when calling it from platform source-set",
    level = DeprecationLevel.HIDDEN
)
actual fun <T> Array<T>.f(): String = <!OVERLOAD_RESOLUTION_AMBIGUITY!>f<!>()

fun <T> Array<out T>.f() = "OK"

fun box(): String {
    if (Array(1) { _ -> ""}.g() != "OK") return "FAIL"

    return Array(0) { _ -> "" }.<!OVERLOAD_RESOLUTION_AMBIGUITY!>f<!>()
}
