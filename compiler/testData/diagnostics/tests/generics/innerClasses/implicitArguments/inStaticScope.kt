// FIR_IDENTICAL
interface Inv<X>
class Outer<E> {
    inner class Inner

    class Nested : Inv<<!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!>>
    inner class Inner2 : Inv<Inner> // no error
    object Obj : Inv<<!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!>>
}

