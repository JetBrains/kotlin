// "Convert parameter to receiver" "true"

impl class Foo {
    impl fun foo(n: Int, <caret>s: String) {

    }
}

fun Foo.test() {
    foo(1, "2")
}