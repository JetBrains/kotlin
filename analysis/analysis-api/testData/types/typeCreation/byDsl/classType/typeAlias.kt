class A

typealias MyAlias = A

fun foo(xx: MyAlias) {
    x<caret_alias>x.toString()
}