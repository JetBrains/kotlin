// "Surround with lambda" "true"
fun foo(action: suspend () -> String) {}

fun usage() {
    foo("oraora"<caret>)
}