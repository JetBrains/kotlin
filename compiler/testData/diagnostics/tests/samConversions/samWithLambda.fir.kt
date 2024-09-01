// LANGUAGE: -JavaTypeParameterDefaultRepresentationWithDNN

// FILE: J.java
public interface J<T> {
    T foo(T x);
}

// FILE: Test.kt

import J

class SamWithLambda {
    fun test1(): J<String?> = J<String?> { x -> x }
    fun test2(): J<String?> = J<String?> { x -> null }
    fun test3(): J<String?> = <!RETURN_TYPE_MISMATCH!>J<String> { x -> x }<!>
    fun test3r(): J<String> = <!RETURN_TYPE_MISMATCH!>J<String?> { x -> x }<!>
    fun test4(): J<String?> = J<String?> { x: String -> x }
    fun test5(): J<String?> = J<String?> { x -> "x" }
    fun test6(): J<String?> = J<String?> { x: String? -> "x" }
    fun test7(): J<String?> = J<String?> { x: String? -> null }
    fun test8(): J<String?> = J { x: String -> null }
    fun test9(): J<String?> = J { x -> x }
    fun test10(): J<String?> = J { x: String? -> "" }
    fun test11(): J<String?> = J { x -> null }
    fun test12(): J<String?> = J { x -> "null" }
    fun test13() = J { x: String -> null }
    fun test14() = J { x: String? -> "null" }
    fun test15() = J<String> { x: String? -> "null" }
    fun test16() = J<String> { x: String -> null }
    fun test17() = J<String> { x -> null }
    fun test18() = J<String> { x -> "null" }
    fun test19() = J<String?> { x: String? -> "null" }
    fun test20() = J<String?> { x: String -> null }
    fun test21() = J<String?> { x -> null }
    fun test22() = J<String?> { x -> "null" }
    fun test23(): J<String> = J { x: String? -> null }
    fun test24(): J<String> = J { x: String -> null }
    fun test25(): J<String> = J { x: String? -> "null" }
    fun test26(): J<String> = J { x: String -> "null" }
}
