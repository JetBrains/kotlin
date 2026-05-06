class MyClass<B: Any> {
    fun <A: Number, D> foo(xx: Any, yy: A, zz: D) {
        x<caret_1_target>x
        y<caret_2_target>y
        z<caret_3_target>z
    }
    fun <C: Int> bar(aa: B, bb: C, cc: B) {
        a<caret_1_base>a
        b<caret_2_base>b
        c<caret_3_base>c
    }
}