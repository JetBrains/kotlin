package usage

import lib.*

fun fail(foo: <error>Foo</error>): <error>Foo</error> {
    <error>bar</error>()
    return <error>foo</error>
}

@ExperimentalAPI
fun ok(foo: Foo): Foo {
    bar()
    return foo
}
