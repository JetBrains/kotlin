interface A<T> : B<T>
interface B<T> : C<Int, T>
interface C<X, Y>

fun <T> test(yy: A<T>, xx: C<Int, String>)  {
    y<caret_1_left>y
    x<caret_1_right>x
}
