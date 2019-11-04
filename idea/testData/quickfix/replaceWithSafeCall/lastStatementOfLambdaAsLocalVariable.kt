// "Replace with safe (?.) call" "true"
fun test(foo: Foo?) {
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