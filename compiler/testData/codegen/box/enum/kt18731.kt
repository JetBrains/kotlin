// IGNORE_BACKEND_FIR: JVM_IR
enum class Bar {
    ONE,
    TWO
}

fun isOne(i: Bar) = i == Bar.ONE

fun box(): String {
    return when (isOne(Bar.ONE) && !isOne(Bar.TWO)) {
        true -> "OK"
        else -> "Failure"
    }
}
