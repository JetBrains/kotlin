// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: JVM_IR
// ^ KT-57755

fun <R : Number> Number.convert(): R = TODO()

fun foo(arg: Number) {
}

fun main(args: Array<String>) {
    val x: Int = 0
    foo(x.convert())
}
