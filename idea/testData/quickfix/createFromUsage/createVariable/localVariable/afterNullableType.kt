// "Create local variable 'foo'" "true"
// ERROR: Variable 'foo' must be initialized

fun test(): Int? {
    val foo: Int?

    return foo
}