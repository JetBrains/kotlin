// !WITH_NEW_INFERENCE
// !LANGUAGE: +ArrayLiteralsInAnnotations +BareArrayClassLiteral

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

@Foo(<!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH{OI}, TYPE_MISMATCH{NI}, TYPE_MISMATCH{NI}!>[""]<!>)
fun test5() {}

@Foo(<!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH{OI}, TYPE_MISMATCH{NI}, TYPE_MISMATCH{NI}!>[Int::class, 1]<!>)
fun test6() {}

@Bar
fun test7() {}
