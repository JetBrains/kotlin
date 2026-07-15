package test

fun interface Predicate<T> {
    fun accept(i: T): T
}

fun interface A: Predicate<Int> {}

typealias MyAlias = A

// sam_constructor: test/MyAlias
