// !LANGUAGE: -RangeUntilOperator

class A {
    operator fun rangeUntil(other: A): Iterable<A> = TODO()
}

fun main(n: A, f: A) {
    for (i in f<!UNSUPPORTED_FEATURE("The feature "range until operator" is only available since language version 1.8")!>..<<!>n) {

    }
}
