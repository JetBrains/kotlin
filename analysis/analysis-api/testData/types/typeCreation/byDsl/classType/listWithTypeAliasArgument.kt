
class B

typealias MyAlias = B

fun foo<T>(xx: MyAlias) {
    x<caret_alias>x.toString()
}