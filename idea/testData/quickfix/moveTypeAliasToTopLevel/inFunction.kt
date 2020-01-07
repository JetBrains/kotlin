// "Move typealias to top level" "true"
fun bar() {
    <caret>typealias Foo = String

    fun baz(foo: Foo) {
    }
}

fun qux() {}