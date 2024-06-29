// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-69201, KT-69069
// Notes: the test shows that deprioritization of deprecated actual doesn't work in case of HMPP project

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

expect fun <T> Array<T>.f<!NO_ACTUAL_FOR_EXPECT{JVM}!>()<!>

// MODULE: intermediate()()(common)
// FILE: intermediate.kt

<!CONFLICTING_OVERLOADS{JVM}!>fun <T> Array<out T>.f()<!> {}

fun test() {
    Array(0) { _ -> "" }.f() // Disambiugation to regular function doesn't work because `actual` function is unreachable from here
}

// MODULE: target()()(intermediate)
// FILE: target.kt

<!CONFLICTING_OVERLOADS!>@Deprecated("It isn't deprioritized because `intermediate` module doesn't know about this `actual`", level = DeprecationLevel.HIDDEN)
actual fun <T> Array<T>.f()<!> {}
