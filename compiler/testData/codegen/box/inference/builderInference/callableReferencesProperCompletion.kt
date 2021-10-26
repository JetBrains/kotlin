// WITH_RUNTIME

fun nonGenericId(x: Any?) = x

fun <T> id(x: T) = x

fun test1(): Sequence<String> = sequence {
    yield("")
    this::class
}

fun <T> sequence2(block: suspend SequenceScope<T>.() -> Unit): Sequence<T> = Sequence { iterator(block) }

fun foo() {
    sequence2<String> {
        id(this::class)
    }
}

fun test2(): Sequence<String> = sequence {
    yield("")
    id(this::class)
}

fun test3(): Sequence<String> = sequence {
    yield("")
    nonGenericId(this::class)
}

fun test4(): Sequence<String> = sequence {
    yield("")
    this::`yield`
}

fun test5(): Sequence<String> = sequence {
    yield("")
    id(this::`yield`)
}

fun test6(): Sequence<String> = sequence {
    yield("")
    nonGenericId(this::`yield`)
}

fun test7(): Sequence<String> = sequence {
    yield("")
    ::`yield`
}

fun test8(): Sequence<String> = sequence {
    yield("")
    id(::`yield`)
}

fun test9(): Sequence<String> = sequence {
    yield("")
    nonGenericId(::`yield`)
}

fun box(): String {
    test1()
    test2()
    test3()
    test4()
    test5()
    test6()
    test7()
    test8()
    test9()
    return "OK"
}
