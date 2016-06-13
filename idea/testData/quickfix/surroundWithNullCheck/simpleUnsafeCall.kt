// "Surround with null check" "true"

fun foo(arg: Int?) {
    arg<caret>.hashCode()
}