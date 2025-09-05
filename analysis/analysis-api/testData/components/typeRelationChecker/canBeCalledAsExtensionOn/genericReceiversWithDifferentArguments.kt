class A<out T>

typealias AInt = A<Int>

typealias AString = A<String>

fun AString.f<caret>oo() {}

fun usage(x: AInt) {
    <expr>x</expr>
}