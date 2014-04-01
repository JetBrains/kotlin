class Foo {
    fun foo(): String {
        return "O"
    }
}

class Bar {
    fun bar(): String {
        return ""
    }
}

var t: String = ""

fun Foo.dot() {
    t = "K"
}

fun box(): String {
    val f: Foo = Foo()
    return f.foo() + t
}
