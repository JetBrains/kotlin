// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR

inline class Name(private val value: String) {
    fun asValue(): String = value
}

fun concat(a: Name, b: Name) = a.asValue() + b.asValue()

inline class UInt(private val value: Int) {
    fun asValue(): Int = value
}

fun box(): String {
    val o = inlinedRun {
        Name("O")
    }

    val k = notInlinedRun {
        Name("K")
    }

    if (concat(o, k) != "OK") return "fail 1"

    val a = UInt(1)
    val one = inlinedRun {
        a
    }

    if (one.asValue() != 1) return "fail 2"

    val b = UInt(2)
    val two = notInlinedRun {
        b
    }

    if (two.asValue() != 2) return "fail 3"

    return "OK"
}

inline fun <R> inlinedRun(block: () -> R): R = block()
fun <R> notInlinedRun(block: () -> R): R = block()