// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun runWithoutReturn(r: () -> Unit) = r()

fun testRun() {
    run {
        1 <!USELESS_CAST!>as Any<!>
        1 <!NI;USELESS_CAST!>as Any<!>
    }

    run<Any> {
        1 <!USELESS_CAST!>as Any<!>
        1 <!USELESS_CAST!>as Any<!>
    }

    fun foo(): Int = 1

    run {
        foo() <!NI;USELESS_CAST!>as Any<!>
    }

    run {
        (if (true) 1 else 2) <!NI;USELESS_CAST!>as Any<!>
    }

    run<Int?> {
        1 <!USELESS_CAST!>as Int<!>
        1 <!USELESS_CAST!>as Int<!>
    }

    runWithoutReturn {
        1 <!USELESS_CAST!>as Any<!>
        1 <!USELESS_CAST!>as Any<!>
    }
}

fun testReturn(): Number {
    run { 1 <!NI;USELESS_CAST!>as Number<!> }
    return run { 1 <!USELESS_CAST!>as Number<!> }
}

fun <T> testDependent() {
    listOf(1).map {
        it <!USELESS_CAST!>as Any<!>
        it <!NI;USELESS_CAST!>as Any<!>
    }

    listOf<T>().map { it <!NI;USELESS_CAST!>as Any?<!> }
}

fun <T> listOf(vararg elements: T): List<T> = TODO()
fun <T, R> Iterable<T>.map(transform: (T) -> R): List<R> = TODO()