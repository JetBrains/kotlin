// !WITH_NEW_INFERENCE
package a

interface A

fun <T> emptyList(): List<T> = throw Exception()

fun test1() {
    <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>emptyList<!>()
}

//--------------

fun <T: A> emptyListOfA(): List<T> = throw Exception()

fun test2() {
    <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>emptyListOfA<!>()
}

//--------------

fun <T: A, R: T> emptyStrangeMap(): Map<T, R> = throw Exception()

fun test3() {
    <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>emptyStrangeMap<!>()
}

//--------------

fun <T, R: T> emptyStrangeMap1(t: T): Map<T, R> = throw Exception("$t")

fun test4() {
    emptyStrangeMap1(1)
}

//--------------

fun <T: A, R> emptyStrangeMap2(t: T): Map<T, R> where R: T = throw Exception("$t")

fun test5(a: A) {
    emptyStrangeMap2(a)
}

//--------------

fun <T: A, R: T> emptyStrangeMap3(r: R): Map<T, R> = throw Exception("$r")

fun test6(a: A) {
    emptyStrangeMap3(a)
}

//--------------

fun <T, R: T> emptyStrangeMap4(l: MutableList<T>): Map<T, R> = throw Exception("$l")

fun test7(list: MutableList<Int>) {
    emptyStrangeMap4(list)
}

//--------------

fun test7() : Map<A, A> = emptyStrangeMap()

//--------------

fun <U, V: U> foo(): U = throw Exception()

fun test8(): Int = foo()
