interface A {
    fun foo()
}

fun <T> myWith(receiver: T, block: T.() -> Unit) {
    receiver.block()
}

fun <T> T.myApply(block: T.() -> Unit) {
    this.block()
}

fun withA(block: A.() -> Unit) {}

fun test_1() {
    withA {
        foo()
    }
}

fun test_2(a: A) {
    myWith(a) {
        foo()
    }
}

fun test_3(a: A) {
    a.myApply {
        foo()
    }
}

fun complexLambda(block: Int.(String) -> Unit) {}

fun test_4() {
    complexLambda {
        inc()
        this.inc()
        it.length
    }
}