// FIX: none
enum class E1

fun test(x: String, y: E1?): Boolean? {
    return x.<caret>equals(y)
}