// ARGUMENT: INV_1

class A<T>

class B

typealias MyAlias = B


val x = <expr>A<Int>()</expr>
fun foo(yy: MyAlias) {
    y<caret_1>y.toString()
}