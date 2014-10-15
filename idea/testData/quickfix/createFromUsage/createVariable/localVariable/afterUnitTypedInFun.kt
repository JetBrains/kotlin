// "Create local variable 'foo'" "true"
// ACTION: Create parameter 'foo'
// ERROR: Variable 'foo' must be initialized

fun test() {
    val foo: Unit

    val u: Unit = foo
}