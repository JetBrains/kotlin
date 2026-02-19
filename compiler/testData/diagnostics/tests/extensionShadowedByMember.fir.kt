// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-54483, KT-75169
// WITH_STDLIB
// LANGUAGE: +ContextParameters

// FILE: test.kt
class C3 {
    fun foo() {}
}

context(_: String)
fun C3.<!EXTENSION_SHADOWED_BY_MEMBER!>foo<!>() {}

abstract class Cache {
    fun get(): Int = 10
    fun get2(): Int = 10
    fun <T> get3(): Int = 10
    fun <T : CharSequence> get4(): Int = 10
}

inline fun <reified T> Cache.get() = 10
fun <T> Cache.get2() = 10
fun <T, R> Cache.get3() = 10
fun <T : List<String>> Cache.<!EXTENSION_SHADOWED_BY_MEMBER!>get4<!>() = 10

class C2 {
    fun foo1(s: String) {}
    fun foo2(s: String, i: Int) {}
    fun foo3(s: String, i: Int, b: Boolean = false) {}

    fun bar(s: String, i: Int, b: Boolean = false) {}
    fun bar2(s: String, i: Int, b: Boolean = false) {}

    fun baz() {}
    fun baz(s: String) {}

    fun qux(s: String) {}
    fun qux(s: String, i: Int) {}
    fun qux(s: String, i: Int, b: Boolean = false) {}

    fun quux(s: String) {}
}

fun C2.foo1(s: String = "") {}
fun C2.foo2(s: String, i: Int = 0) {}
fun C2.foo3(s: String, i: Int = 0, b: Boolean = false) {}

fun C2.<!EXTENSION_SHADOWED_BY_MEMBER!>bar<!>(s: String, i: Int, b: Boolean) {}
fun C2.<!EXTENSION_SHADOWED_BY_MEMBER!>bar2<!>(s: String, i: Int, b: Boolean = true) {}

fun C2.baz(s: String = "") {}

fun C2.qux(s: String = "") {}
fun C2.qux(s: String, i: Int = 0) {}
fun C2.qux(s: String, i: Int = 0, b: Boolean = false) {}

fun C2.quux(s: String, b: Boolean = false) {}

class C4 {
    fun foo(s: String) {}
}

fun C4.foo(x: String) {}

fun J.foo(s: String) {}

// FILE: J.java
public class J {
    public void foo(String s) {}
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext,
inline, integerLiteral, javaType, nullableType, reified, stringLiteral, typeConstraint, typeParameter */
