var result = ""
fun result(r: String) { result = r }

object Foo {
    private fun String.plus() = "(" + this + ")"

    fun foo() = { result(+"Stuff") }()
}

fun box(): String {
    Foo.foo()
    return if (result == "(Stuff)") "OK" else "Fail $result"
}
