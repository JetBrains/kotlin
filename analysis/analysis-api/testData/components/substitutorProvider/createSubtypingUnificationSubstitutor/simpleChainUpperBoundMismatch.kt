interface A<T> : B<T> // [UPPER_BOUND_VIOLATED] Type argument is not within its bounds: must be subtype of 'Int'.
interface B<S : Int> : C<Int, S>
interface C<X, Y>

fun <K> test(yy: A<K>, xx: C<Int, String>) {
    y<caret_1_left>y
    x<caret_1_right>x
}
