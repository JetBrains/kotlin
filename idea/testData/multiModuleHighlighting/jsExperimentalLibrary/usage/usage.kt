package usage

import lib.*

fun fail(foo: <error>Foo</error>): <error>Foo</error> {
    <error>bar</error>()
    return foo
}

@ExperimentalAPI
fun ok(foo: Foo): Foo {
    bar()
    return foo
}
