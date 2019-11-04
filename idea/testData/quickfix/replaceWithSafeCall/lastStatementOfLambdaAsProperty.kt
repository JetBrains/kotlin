// "Replace with safe (?.) call" "true"
class Test(private val foo: Foo?) {
    val baz = {
        bar("")
        bar("")
        foo<caret>.s
    }
}

class Foo {
    val s = ""
}

fun bar(s: String) {}