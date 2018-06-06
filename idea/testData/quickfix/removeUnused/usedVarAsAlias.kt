// "Safe delete 'usedVar'" "false"
// ACTION: Add getter
// ACTION: Add getter and setter
// ACTION: Add setter
// ACTION: Specify type explicitly
import usedVar as used

var <caret>usedVar = 0

fun foo() {
    used++
}