// "Convert receiver to parameter" "true"

impl class Foo {
    impl fun <caret>String.foo(n: Int) {

    }
}

fun Foo.test() {
    "1".foo(2)
}