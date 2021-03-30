// A
open class B<F>
interface C<E>
abstract class A<T : A<T>> : B<Collection<T>>(), C<T> {
    inner open class Inner<D> : B<Collection<T>>(), C<D> {
    }

    inner class Inner2<X> : Inner<X>(), C<X>
}

