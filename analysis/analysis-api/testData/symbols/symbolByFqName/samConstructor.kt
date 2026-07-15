package test

fun interface Predicate<T> {
    fun accept(i: T): T
}

fun interface A: Predicate<Int> {}

// sam_constructor: test/A
