// "Replace with safe (?.) call" "true"
fun test(foo: Foo?) {
    bar(1, foo<caret>.s, 2)
}

class Foo {
    val s = ""
}

fun bar(i: Int, s: String, j: Int) {}