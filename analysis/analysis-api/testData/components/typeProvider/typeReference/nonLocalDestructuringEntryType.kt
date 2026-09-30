// KT-74654
// KT-89821: InvalidFirElementTypeException, the type reference has no FIR
// IGNORE_FIR
class C {
    val (a: In<caret>t, b) = 1 to 2
}
