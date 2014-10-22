// KT-6026 Exception on instantiating a nested class in an anonymous object

val oo = object {
    class Nested

    fun f1() = Nested(<!TOO_MANY_ARGUMENTS!>11<!>)
}
