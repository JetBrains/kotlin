class Foo {
    fun foo(): String {
        return "FAIL"
    }
}

class Bar {
    fun bar(): String {
        return "O"
    }
}

var t: String = ""

fun Foo.dot(): Bar {
    t = "K"
    return Bar()
}

fun box(): String {
    val f: Foo = Foo()
    return f.bar() + t
}
