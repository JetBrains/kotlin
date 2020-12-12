// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

class Num<T : Number>
class NumColl<T : Collection<Number>>
class TColl<T, C : Collection<T>>

typealias NA<T> = Num<T>
typealias NL<T2> = NumColl<List<T2>>
typealias MMMM<A3> = NL<A3>
typealias TC<T1, T2> = TColl<T1, T2>

fun test1(x: NA<Int>) {}
fun test2(x: NA<Any>) {}
fun test3(x: NL<Int>) {}
fun test4(x: NL<Any>) {}

val test5 = NA<Int>()
val test6 = NA<Any>()
val test7 = NL<Int>()
val test8 = MMMM<Int>()
val test9dwd = NL<Any>()

fun test9(x: TC<Number, Collection<Number>>) {}
fun test10(x: TC<Number, Collection<Int>>) {}
fun test11(x: TC<Number, List<Int>>) {}
fun test12(x: TC<Number, List<Any>>) {}

val test13 = TC<Number, Collection<Number>>()
val test14 = TC<Number, Collection<Int>>()
val test15 = TC<Number, List<Int>>()
val test16 = TC<Number, List<Any>>()
