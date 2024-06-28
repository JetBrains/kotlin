class Foo(val box: String = "OK")

fun box(): String {
    return Foo().box
}