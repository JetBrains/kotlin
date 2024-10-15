// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

interface Test1 {
    fun test1() {}
}
fun Test1.<!EXTENSION_SHADOWED_BY_MEMBER!>test1<!>() {}

interface Test2 {
    fun test2(x: String) {}
}
fun Test2.test2(x: Any) {}

interface Test3 {
    fun test3(x: Any) {}
}
fun Test3.<!EXTENSION_SHADOWED_BY_MEMBER!>test3<!>(x: String) {}
fun <T : Any?> Test3.test3(x: T) {}

interface Test4 {
    fun <T> test4(x: T) {}
}
fun Test4.<!EXTENSION_SHADOWED_BY_MEMBER!>test4<!>(x: String) {}

interface Test5 {
    fun <T> test5(x: T) {}
}
fun <T : Number> Test5.<!EXTENSION_SHADOWED_BY_MEMBER!>test5<!>(x: T) {}

interface Test6 {
    fun <T : List<Any>> test6(x: T) {}
}
fun <T : Set<Any>> Test6.test6(x: T) {}

interface Test7 {
    fun test7()
}

fun Test7?.test7() {}

interface Test8 {
    fun test8()
}

typealias NullableTest8 = Test8?

fun NullableTest8.test8() {}