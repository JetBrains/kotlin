package c

fun test1() {
    val r: Nothing = null!!
}

fun test2(a: A) {
    a + a
    bar()
}

fun test3() {
    null!!
    bar()
}

fun throwNPE(): Nothing = null!!

class A {
    operator fun plus(a: A): Nothing = throw Exception()
}

fun bar() {}