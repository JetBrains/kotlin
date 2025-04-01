// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-54483, KT-75169
// WITH_STDLIB
// LANGUAGE: +ContextParameters

class C3 {
    fun foo() {}
}

context(_: String)
fun C3.foo() {}

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
