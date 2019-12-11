interface Inv<X>
class Outer<E> {
    inner class Inner

    class Nested : Inv<Inner>
    object Obj : Inv<Inner>
}
