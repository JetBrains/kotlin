class A<T, K>

typealias MyAlias<K> = A<Int, K>

fun foo<K>(yy: MyAlias<K>) {
    y<caret_alias>y.toString()
}