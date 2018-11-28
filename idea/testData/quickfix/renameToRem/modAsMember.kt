// "Rename to 'rem'" "true"
// DISABLE-ERRORS

object Rem {
    operator<caret> fun mod(x: Int) {}
    operator fun modAssign(x: Int) {}
}

fun test() {
    Rem % 1
    Rem.mod(1)
    Rem.modAssign(1)
}