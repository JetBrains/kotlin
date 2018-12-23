// "Remove type parameters" "true"

fun test() {
    val <caret><T : unresovled_reference, K> x = 0
}