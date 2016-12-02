class My {
    fun foo(a: Int, b: String) = "$b$a"

}

fun baz(f: My.(Int, String) -> String) = My().f(42, "")

val x = baz(<caret>My::foo)
