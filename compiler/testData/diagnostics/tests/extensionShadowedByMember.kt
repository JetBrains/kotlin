// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-54483, KT-75169
// WITH_STDLIB
// LANGUAGE: +ContextParameters

class C3 {
    fun foo() {}
}

<!CONTEXT_PARAMETERS_UNSUPPORTED!>context(_: <!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>)<!>
fun C3.<!EXTENSION_SHADOWED_BY_MEMBER!>foo<!>() {}

abstract class Cache {
    fun get(): Int = 10
    fun get2(): Int = 10
    fun <T> get3(): Int = 10
    fun <T : CharSequence> get4(): Int = 10
}

inline fun <reified T> Cache.<!EXTENSION_SHADOWED_BY_MEMBER!>get<!>() = 10
fun <T> Cache.<!EXTENSION_SHADOWED_BY_MEMBER!>get2<!>() = 10
fun <T, R> Cache.<!EXTENSION_SHADOWED_BY_MEMBER!>get3<!>() = 10
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

fun C2.<!EXTENSION_SHADOWED_BY_MEMBER!>foo1<!>(s: String = "") {}
fun C2.<!EXTENSION_SHADOWED_BY_MEMBER!>foo2<!>(s: String, i: Int = 0) {}
fun C2.<!EXTENSION_SHADOWED_BY_MEMBER!>foo3<!>(s: String, i: Int = 0, b: Boolean = false) {}

fun C2.<!EXTENSION_SHADOWED_BY_MEMBER!>bar<!>(s: String, i: Int, b: Boolean) {}
fun C2.<!EXTENSION_SHADOWED_BY_MEMBER!>bar2<!>(s: String, i: Int, b: Boolean = true) {}

fun C2.<!EXTENSION_SHADOWED_BY_MEMBER!>baz<!>(s: String = "") {}

fun C2.<!EXTENSION_SHADOWED_BY_MEMBER!>qux<!>(s: String = "") {}
fun C2.<!EXTENSION_SHADOWED_BY_MEMBER!>qux<!>(s: String, i: Int = 0) {}
fun C2.<!EXTENSION_SHADOWED_BY_MEMBER!>qux<!>(s: String, i: Int = 0, b: Boolean = false) {}

fun C2.quux(s: String, b: Boolean = false) {}