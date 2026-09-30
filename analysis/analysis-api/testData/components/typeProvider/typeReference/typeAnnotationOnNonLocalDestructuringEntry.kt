// KT-74654
// KT-89821: InvalidFirElementTypeException, the type reference has no FIR
// IGNORE_FIR
@Target(AnnotationTarget.TYPE)
annotation class Ann

class C {
    val (a: @An<caret>n Int, b) = 1 to 2
}
