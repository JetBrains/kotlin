class MyClass<B: Any> {
    fun <A: Number> foo(xx: A) {
        x<caret_1_target>x
    }
    fun bar(yy: B) {
        y<caret_1_base>y
    }
}