class A<T, K>

typealias MyAlias<K> = A<Int, K>

fun foo<K>(xx: Int, yy: MyAlias<K>) {
    x<caret_argument>x.toString()
    y<caret_alias>y.toString()
}