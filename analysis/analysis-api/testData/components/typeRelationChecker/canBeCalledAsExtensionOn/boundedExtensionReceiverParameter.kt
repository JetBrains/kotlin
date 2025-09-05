interface MyClass<A>

fun <T: X, X: R, R: Number> MyClass<T>.ex<caret>t() {}

fun usage(x: MyClass<Int>) {
    <expr>x</expr>
}