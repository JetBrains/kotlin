class MyClass<B: Any> {
    fun <A: Number> foo(xx: A) {
        x<caret_1_right>x
    }
    fun bar(yy: B) {
        y<caret_1_left>y
    }
}
