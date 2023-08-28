// WITH_STDLIB

// MUTE_SIGNATURE_COMPARISON_K2: JVM_IR
// ^ KT-57755

fun main() {
    val (a: Any, _) = 1 to 2
    println(a)
}
