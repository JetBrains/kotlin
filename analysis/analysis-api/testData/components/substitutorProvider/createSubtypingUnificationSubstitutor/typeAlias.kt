interface A<T> : B<T>
interface B<T> : C<Int, T>
interface C<X, Y>

typealias CAlias<P, Q> = C<P, Q>

fun <T> test(yy: A<T>, xx: CAlias<Int, String>)  {
    y<caret_1_left>y
    x<caret_1_right>x
}
