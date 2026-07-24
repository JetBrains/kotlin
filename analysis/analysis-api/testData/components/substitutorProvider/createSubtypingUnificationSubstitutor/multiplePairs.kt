class MyClass<B: Any> {
    fun <A: Number, D> foo(xx: Any, yy: A, zz: D) {
        x<caret_1_right>x
        y<caret_2_right>y
        z<caret_3_right>z
    }
    fun <C: Int> bar(aa: B, bb: C, cc: B) {
        a<caret_1_left>a
        b<caret_2_left>b
        c<caret_3_left>c
    }
}
