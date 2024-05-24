package test

interface Foo

interface WithOperator {
    operator fun plusAssign(f: Foo)
}

fun test(withOperator: WithOperator, foo: Foo) {
    withOperator <caret>+= foo
}
