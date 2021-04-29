// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun runWithoutReturn(r: () -> Unit) = r()

fun testRun() {
    run {
        1 as Any
        1 as Any
    }

    run<Any> {
        1 as Any
        1 as Any
    }

    fun foo(): Int = 1

    run {
        foo() as Any
    }

    run {
        (if (true) 1 else 2) as Any
    }

    run<Int?> {
        1 <!USELESS_CAST!>as Int<!>
        1 <!USELESS_CAST!>as Int<!>
    }

    runWithoutReturn {
        1 as Any
        1 as Any
    }
}

fun testReturn(): Number {
    run { 1 as Number }
    return run { 1 as Number }
}

fun <T> testDependent() {
    listOf(1).map {
        it as Any
        it as Any
    }

    listOf<T>().map { it as Any? }
}

fun <T> listOf(vararg elements: T): List<T> = TODO()
fun <T, R> Iterable<T>.map(transform: (T) -> R): List<R> = TODO()
