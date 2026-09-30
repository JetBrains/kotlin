// KT-74654
// KT-89821: InvalidFirElementTypeException, the type reference has no FIR
// IGNORE_FIR
annotation class Ann

class C {
    val (@An<caret>n a, b) = 1 to 2
}
