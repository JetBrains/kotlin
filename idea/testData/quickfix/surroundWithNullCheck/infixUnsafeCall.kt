// "Surround with null check" "true"

infix fun Int.op(arg: Int) = this

fun foo(arg: Int?) {
    arg <caret>op 42
}