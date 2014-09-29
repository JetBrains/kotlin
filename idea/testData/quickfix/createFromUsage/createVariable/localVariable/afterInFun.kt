// "Create local variable 'foo'" "true"
// ACTION: Create parameter 'foo'
// ERROR: Variable 'foo' must be initialized

fun test(): Int {
    val foo: Int

    return foo
}