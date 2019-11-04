// "Replace with safe (?.) call" "true"
fun test(foo: Foo?): String {
    return foo<caret>.s
}

class Foo {
    val s = ""
}