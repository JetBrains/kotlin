class MyClass<A: Any> {
    fun foo(xx: A, yy: A) {
        x<caret_1_right>x
        y<caret_2_right>y
    }
    fun <B: Number, C: String> bar(aa: B, bb: C) {
        a<caret_1_left>a
        b<caret_2_left>b
    }
}
