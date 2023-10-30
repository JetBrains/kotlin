// !LANGUAGE: +DefinitelyNonNullableTypes

import kotlin.reflect.KProperty1

class Mem
class Ext

class Foo<T> {
    fun foo(prop: KProperty1<T & Any, *>): Mem = Mem()
}
fun <T> Foo<T>.bar(prop: KProperty1<T & Any, *>): Ext = Ext()

class Bar<T> {
    fun bar(prop: KProperty1<T & Any, *>): Mem = Mem()
}
fun <T> Bar<T>.bar(prop: KProperty1<T & Any, *>): Ext = Ext()

class Baz<T> {
    fun baz(prop: KProperty1<T, *>): Mem = Mem()
}
fun <T> Baz<T>.baz(prop: KProperty1<T & Any, *>): Ext = Ext()

fun <T> id(t: T): T = t

fun main() {
    val r01: Mem = Foo<String?>().foo(String::length)
    val r02: Mem = Foo<String?>().foo(id(String::length))

    val r03: Ext = Foo<String?>().bar(String::length)
    val r04: Ext = Foo<String?>().bar(id(String::length))

    val r05: Mem = Bar<String?>().bar(String::length)
    val r06: Mem = Bar<String?>().bar(id(String::length))

    val r07 = <!DEBUG_INFO_EXPRESSION_TYPE("Ext")!>Baz<String?>().baz(String::length)<!>
    val r08: Ext = Baz<String?>().baz(id(String::length))
}
