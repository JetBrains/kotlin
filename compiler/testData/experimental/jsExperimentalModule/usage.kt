package usage

import lib.*

fun fail(foo: Foo) {
    bar()
}

@ExperimentalAPI
fun ok(foo: Foo) {
    bar()
}
