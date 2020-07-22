package rest

abstract class Foo<T> {
    abstract val x: T<<!TYPE_ARGUMENTS_NOT_ALLOWED!>Int<!>>

    abstract fun foo(): T<<!TYPE_ARGUMENTS_NOT_ALLOWED!>String<!>>
}

fun <T> foo() {
    bar<<!UPPER_BOUND_VIOLATED!>T<<!TYPE_ARGUMENTS_NOT_ALLOWED!>Int<!>><!>>()
}

fun <T> bar() {}