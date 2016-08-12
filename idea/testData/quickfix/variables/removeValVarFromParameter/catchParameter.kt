// "Remove 'val' from parameter" "true"
// WITH_RUNTIME
fun f() {
    try {

    } catch (<caret>val e: Exception) {

    }
}