interface Inv<X>
class Outer<E> {
    inner class Inner

    class Nested : Inv<<!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!>>
    object Obj : Inv<<!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!>>
}
