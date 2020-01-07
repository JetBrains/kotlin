// "Move typealias to top level" "true"
class C {
    <caret>typealias Foo = String

    fun bar(foo: Foo) {
    }
}

fun baz() {}