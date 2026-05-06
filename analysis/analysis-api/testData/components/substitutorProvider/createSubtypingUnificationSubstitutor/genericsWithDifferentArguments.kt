class A<out T>

typealias AInt = A<Int>

typealias AString = A<String>

fun usage(xx: AInt, yy: AString) {
    x<caret_1_base>x
    y<caret_1_target>y
}
