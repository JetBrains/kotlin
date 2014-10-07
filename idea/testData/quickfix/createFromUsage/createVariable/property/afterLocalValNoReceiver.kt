// "Create property 'foo' from usage" "true"

fun test() {
    val foo: Int

    fun nestedTest(): Int {
        return foo
    }
}
