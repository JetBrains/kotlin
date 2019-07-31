package foobar

import bar.baz
import d0.AnotherSupertype

fun <T : Any> expectAnotherSupertype(x: T, y: T): T {
    x.hashCode() + y.hashCode()
    return x
}

fun foo(x: AnotherSupertype) {
    x.hashCode()
}

fun main(x: AnotherSupertype) {
    baz().foo()
    // TODO: support subtyping too
    foo(baz())

    expectAnotherSupertype(x, baz()).another()
}
