// IGNORE_BACKEND: JVM_IR
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

// 1 IF_ACMPNE
// 0 INVOKESTATIC kotlin/jvm/internal/Intrinsics.areEqual \(Ljava/lang/Object;Ljava/lang/Object;\)Z