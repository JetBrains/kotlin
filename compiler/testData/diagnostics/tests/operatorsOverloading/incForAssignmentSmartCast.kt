// SKIP_TXT
var c = 1

fun nullable(): Int? = null

fun foo(): Int {
    var x = nullable()
    if (x == null) {
        x = c++
    }

    return <!DEBUG_INFO_SMARTCAST!>x<!>
}
