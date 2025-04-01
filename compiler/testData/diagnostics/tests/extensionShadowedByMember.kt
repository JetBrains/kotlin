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
    fun foo(s: String) {}
    fun foo(s: String, i: Int) {}
    fun foo(s: String, i: Int, b: Boolean = false) {}

    fun bar(s: String, i: Int, b: Boolean = false) {}
}

fun C2.<!EXTENSION_SHADOWED_BY_MEMBER!>foo<!>(s: String = "") {}
fun C2.<!EXTENSION_SHADOWED_BY_MEMBER!>foo<!>(s: String, i: Int = 0) {}
fun C2.<!EXTENSION_SHADOWED_BY_MEMBER!>foo<!>(s: String, i: Int = 0, b: Boolean = false) {}

fun C2.<!EXTENSION_SHADOWED_BY_MEMBER!>bar<!>(s: String, i: Int, b: Boolean) {}