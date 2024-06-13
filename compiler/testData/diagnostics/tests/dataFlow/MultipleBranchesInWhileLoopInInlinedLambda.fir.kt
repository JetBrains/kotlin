// DIAGNOSTICS: +UNUSED_VARIABLE, +UNUSED_VALUE
// WITH_EXTENDED_CHECKERS

inline fun execute(func: () -> Unit) { func() }
fun conditionA(): Boolean { return false }
fun conditionB(): Boolean { return true }

fun main() {
    execute {
        var value = 0
        while (true) {
            if (conditionA()) return
            if (conditionB()) {
                value.run { if (this > 0) return@execute }
                value = 42
            }
        }
    }
}
