// FIR_IDENTICAL
// LANGUAGE: +WarnAboutNonExhaustiveWhenOnAlgebraicTypes
fun test1() {
    if (true) {
        <!NO_ELSE_IN_WHEN!>when<!> (true) {
            true -> println()
        }
    } else {
        System.out?.println() // kotlin.Unit?
    }
}

fun test2() {
    val mlist = arrayListOf("")
    if (true) {
        <!NO_ELSE_IN_WHEN!>when<!> (true) {
            true -> println()
        }
    } else {
        mlist.add("") // kotlin.Boolean
    }
}
