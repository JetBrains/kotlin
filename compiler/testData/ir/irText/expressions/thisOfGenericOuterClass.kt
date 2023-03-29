// MUTE_SIGNATURE_COMPARISON_K2: JVM_IR
// ^ KT-57429

class Outer<T>(val x: T) {
    open inner class Inner(val y: Int)
}

fun Outer<Int>.test() =
        object : Outer<Int>.Inner(42) {
            val xx = x + y
        }
