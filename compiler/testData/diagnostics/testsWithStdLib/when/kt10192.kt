// FIR_IDENTICAL
// LANGUAGE: +WarnAboutNonExhaustiveWhenOnAlgebraicTypes
fun test1() {
    if (true) {
        <!NON_EXHAUSTIVE_WHEN_STATEMENT!>when<!> (true) {
            true -> println()
        }
    } else {
        System.out?.println() // kotlin.Unit?
    }
}

fun test2() {
    val mlist = arrayListOf("")
    if (true) {
        <!NON_EXHAUSTIVE_WHEN_STATEMENT!>when<!> (true) {
            true -> println()
        }
    } else {
        mlist.add("") // kotlin.Boolean
    }
}
