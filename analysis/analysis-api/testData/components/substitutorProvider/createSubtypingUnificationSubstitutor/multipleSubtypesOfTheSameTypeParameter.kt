class MyClass<A: Any> {
    fun foo(xx: A, yy: A) {
        x<caret_1_target>x
        y<caret_2_target>y
    }
    fun <B: Number, C: String> bar(aa: B, bb: C) {
        a<caret_1_base>a
        b<caret_2_base>b
    }
}