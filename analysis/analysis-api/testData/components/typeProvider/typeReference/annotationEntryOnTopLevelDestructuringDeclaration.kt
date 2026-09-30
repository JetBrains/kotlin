// KT-74654: InvalidFirElementTypeException, the annotation has no FIR
// IGNORE_FIR
annotation class Ann

@An<caret>n
val (a, b) = 1 to 2
