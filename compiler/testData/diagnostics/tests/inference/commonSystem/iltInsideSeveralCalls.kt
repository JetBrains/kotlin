// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER -UNCHECKED_CAST -UNUSED_EXPRESSION

fun <T> consumeLongAndMaterialize(x: Long): T = null as T
fun consumeAny(x: Any) = x

fun main() {
    consumeAny(consumeLongAndMaterialize(3L * 1000))

    if (true) {
        consumeLongAndMaterialize(3L * 1000)
    } else true
}
