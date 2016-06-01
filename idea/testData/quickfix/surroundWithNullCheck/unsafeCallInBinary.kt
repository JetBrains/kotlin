// "Surround with null check" "true"

fun foo(arg: Int?) {
    42 + arg<caret>.hashCode() - 13
}