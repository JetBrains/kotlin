// "Rename to 'rem'" "true"
// DISABLE-ERRORS

object A
operator<caret> fun A.mod(x: Int) {}

fun test() {
    A.mod(3)
    A % 2
}