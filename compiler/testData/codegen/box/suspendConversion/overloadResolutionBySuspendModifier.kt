// !LANGUAGE: +SuspendConversion
// !DIAGNOSTICS: -UNUSED_PARAMETER
// IGNORE_BACKEND: JVM
// IGNORE_LIGHT_ANALYSIS
//  java.lang.IllegalStateException: OVERLOAD_RESOLUTION_AMBIGUITY: Overload resolution ambiguity:
//  public fun foo(x: () -> Int): Unit defined in root package in file overloadResolutionBySuspendModifier.kt
//  public fun foo(x: suspend () -> Int): Unit defined in root package in file overloadResolutionBySuspendModifier.kt (15,5) in /overloadResolutionBySuspendModifier.kt

var foo1 = false
var foo2 = false

fun foo(x: () -> Int) { foo1 = true }
fun foo(x: suspend () -> Int) { foo2 = true}

fun usualCall(): Int = 42
suspend fun suspendCall(): Int = 42

fun test2(f: () -> Int, g: suspend () -> Int) {
    foo(f)
    foo(g)
}

fun box(): String {
    test2({ 1 }, { 2 })
    return if (foo1 && foo2)
        "OK"
    else
        "foo1: $foo1; foo2: $foo2"
}