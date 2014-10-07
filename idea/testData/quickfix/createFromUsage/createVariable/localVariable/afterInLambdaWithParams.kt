// "Create local variable 'foo'" "true"
// ACTION: Create parameter 'foo'
// ERROR: Variable 'foo' must be initialized

fun test(n: Int) {
    val f: (Int, Int) -> Int = { (a, b) ->
        val foo: Int

        foo
    }
}