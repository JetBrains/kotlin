// KT-6026 Exception on instantiating a nested class in an anonymous object

val oo = object {
    // Forbidden in KT-13510
    class Nested

    fun f1() = <!UNRESOLVED_REFERENCE!>Nested<!>(11)
}
