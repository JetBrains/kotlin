enum class Bar {
    ONE {
        override fun toString(): String {
            if (this != TWO && this == ONE) return "OK" else return "FAIL"
        }
    },
    TWO;
}

fun box(): String {
    return Bar.ONE.toString()
}

// 1 IF_ACMPNE
// 1 IF_ACMPEQ
// 0 INVOKESTATIC kotlin/jvm/internal/Intrinsics.areEqual \(Ljava/lang/Object;Ljava/lang/Object;\)Z