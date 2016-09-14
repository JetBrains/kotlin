interface Inv<X>
class Outer<E> {
    inner class Inner

    class Nested : Inv<<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inner<!>>
    object Obj : Inv<<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inner<!>>
}
