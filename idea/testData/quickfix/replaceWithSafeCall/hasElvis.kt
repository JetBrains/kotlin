// "Replace with safe (?.) call" "true"
// WITH_RUNTIME

fun main() {
    var a = foo()<caret>.length ?: 0
}

fun foo(): String? {
    return ""
}
