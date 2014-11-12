// "Create class 'Foo'" "true"

fun test() {
    fun nestedTest() = Foo(2, "2")
}

class Foo(i: Int, s: String) {

}
