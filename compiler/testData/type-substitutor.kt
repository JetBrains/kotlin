class ___Context<T: Any, R> {
  fun tr(t: T): R
}

fun <X> x(x: X): X

fun <X> cx(x: C<X>): C<X>

class C<X>
class In<in T>
class Out<out T>
class P<X1, X2>

class Rec<T: Rec<T>>