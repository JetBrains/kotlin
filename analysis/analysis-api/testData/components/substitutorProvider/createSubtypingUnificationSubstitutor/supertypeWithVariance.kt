interface A<T>

class B: A<Int>

fun usage(xx: B, yy: A<in Nothing>) {
    x<caret_1_base>x
    y<caret_1_target>y
}
