// "Safe delete 'usedVar'" "false"
// ACTION: Specify type explicitly
import usedVar as used

var <caret>usedVar = 0

fun foo() {
    used++
}