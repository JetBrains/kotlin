// FIX: Convert to object declaration
// WITH_RUNTIME

class <caret>A {
    companion object {
        val prop = "test"
    }
}

fun main() {
    println(A.Companion.prop)
    println(A.prop)
}
