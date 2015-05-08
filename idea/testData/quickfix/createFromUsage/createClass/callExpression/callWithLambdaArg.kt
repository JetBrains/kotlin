// "Create class 'Foo'" "true"

fun test() {
    val a = <caret>Foo(2, "2") { p: Int -> p + 1 }
}