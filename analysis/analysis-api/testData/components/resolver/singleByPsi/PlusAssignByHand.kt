package test

interface Foo

interface WithOperator {
    operator fun plus(f: Foo): WithOperator
}

fun test(withOperator: WithOperator, foo: Foo) {
    var variable = withOperator
    variable <caret>= variable + foo
}
