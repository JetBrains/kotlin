// !LANGUAGE: +BareArrayClassLiteral

import kotlin.reflect.KClass

annotation class Foo(val a: Array<KClass<*>> = [])

class Gen<T>

annotation class Bar(val a: Array<KClass<*>> = [Int::class, Array<Int>::class, Gen::class])

@Foo([])
fun test1() {}

@Foo([Int::class, String::class])
fun test2() {}

@Foo([Array::class])
fun test3() {}

@Foo([<!CLASS_LITERAL_LHS_NOT_A_CLASS!>Gen<Int>::class<!>])
fun test4() {}

@Foo(<!ARGUMENT_TYPE_MISMATCH!>[""]<!>)
fun test5() {}

@Foo(<!ARGUMENT_TYPE_MISMATCH!>[Int::class, 1]<!>)
fun test6() {}

@Bar
fun test7() {}
