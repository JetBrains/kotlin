interface Foo<T>

class Bar {
    operator fun <T> invoke(): Foo<T> = throw Exception()
}

class A {
    val bar = Bar()
}

fun fooInt(l: Foo<Int>) = l

fun test(bar: Bar, a: A) {
    // no elements with error types
    fooInt((bar()))
    fooInt(if (true) bar() else bar())
    fooInt(<!REDUNDANT_LABEL_WARNING!>label@<!> bar())
    fooInt(a.bar())
    fooInt(((<!REDUNDANT_LABEL_WARNING!>label@<!> if (true) (a.bar()) else bar())))
}