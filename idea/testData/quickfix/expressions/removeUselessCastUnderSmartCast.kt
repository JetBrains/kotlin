// "Remove useless cast" "true"
fun test(x: Any): String? {
    if (x is String) {
        return x <caret>as String
    }
    return null
}