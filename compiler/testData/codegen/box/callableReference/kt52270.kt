// WITH_STDLIB

import kotlin.random.Random

fun fun1() {
}

fun fun2() {
}

fun takesLambda(lambda: () -> Unit) {
    lambda()
}

fun takesOtherLambda(lambda: () -> Unit) = lambda()

fun foo() {}

fun bar2(): Int = 1
fun <K> foo4(): K = 2 as K

fun test1(): String {
    takesOtherLambda {
        val reference: () -> Unit = if (Random.nextBoolean()) {
            ::fun1
        } else {
            ::fun2
        }
        takesLambda(reference)
    }
    return "OK"
}

fun test2(): String {
    takesOtherLambda {
        val reference: () -> Unit = if (Random.nextBoolean())
            ::fun1
        else
            ::fun2
        takesLambda(reference)
    }
    return "OK"
}

fun test3(): String {
    takesOtherLambda {
        val reference: () -> Unit = if (Random.nextBoolean()) {
            foo()
            ::fun1
        } else {
            foo()
            ::fun2
        }
        takesLambda(reference)
    }
    return "OK"
}

fun box(): String {
    return if (test1() == "OK" && test2() == "OK" && test3() == "OK") "OK" else "NOK"
}