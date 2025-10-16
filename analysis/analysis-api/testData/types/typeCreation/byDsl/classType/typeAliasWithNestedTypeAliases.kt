class A<T>
class B<T>
class C<T, K>

typealias AliasA<T> = A<T>
typealias AliasB = B<*>
typealias AliasC<T, K> = C<T, K>
typealias MyAlias<T> = AliasC<AliasB, AliasA<T>>

fun foo<T>(xx: MyAlias<T>) {
    x<caret_alias>x.toString()
}