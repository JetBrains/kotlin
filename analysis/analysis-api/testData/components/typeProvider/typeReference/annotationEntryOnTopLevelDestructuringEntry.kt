// KT-74654
// KT-90199: InvalidFirElementTypeException, the type reference has no FIR
// IGNORE_FIR
annotation class Ann

val (a, @An<caret>n b) = 1 to 2
