// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// SKIP_JDK6
// SAM_CONVERSIONS: CLASS
// FILE: test.kt
// Test that SAM wrappers with type parameters are cached properly.
class A {
    fun stringPredicate(string: String, p: (String) -> Boolean): Boolean {
        return java.util.function.Predicate<String>(p).test(string)
    }

    fun intPredicate(int: Int, p: (Int) -> Boolean): Boolean {
        return java.util.function.Predicate<Int>(p).test(int)
    }
}

fun wrapStringPredicate(p: (String) -> Boolean): java.util.function.Predicate<String> =
    java.util.function.Predicate<String>(p)

fun wrapIntPredicate(p: (Int) -> Boolean): java.util.function.Predicate<Int> =
    java.util.function.Predicate<Int>(p)

fun box(): String {
    if (!A().stringPredicate("OK") { it == "OK"}) return "Fail 1"
    if (!A().intPredicate(0) { it == 0 }) return "Fail 2"

    try {
        java.lang.Class.forName("TestKt\$sam\$java_util_function_Predicate$0")
    } catch (e: Throwable) {
        return "Fail 3: sam wrapper not found"
    }

    val stringPredicateWrapperClass = wrapStringPredicate { true }::class.java
    val intPredicateWrapperClass = wrapIntPredicate { false }::class.java
    if (stringPredicateWrapperClass !== intPredicateWrapperClass)
        return "Fail 4: sam wrapper not cached"

    return "OK"
}
