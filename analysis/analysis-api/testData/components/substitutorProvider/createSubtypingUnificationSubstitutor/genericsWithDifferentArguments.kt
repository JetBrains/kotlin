class A<out T>

typealias AInt = A<Int>

typealias AString = A<String>

fun usage(xx: AInt, yy: AString) {
    x<caret_1_left>x
    y<caret_1_right>y
}
