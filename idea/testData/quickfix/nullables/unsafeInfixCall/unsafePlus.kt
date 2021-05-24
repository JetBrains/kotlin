// "Replace with safe (?.) call" "true"

operator fun Int.plus(index: Int) = this
fun fox(arg: Int?) = arg <caret>+ 42