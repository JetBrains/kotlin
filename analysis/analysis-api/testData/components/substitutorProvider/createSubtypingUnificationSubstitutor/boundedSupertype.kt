interface MyClass<A>

fun <T: X, X: R, R: Number> MyClass<T>.ext() {
    thi<caret_1_right>s
}

fun usage(x: MyClass<Int>) {
    <caret_1_left>x
}
