// LANGUAGE: +ProperIeee754Comparisons
fun box(): String {
    val zero: Any = 0.0
    val floatZero: Any = -0.0F
    if (zero is Double && floatZero is Float) {
        if (zero == floatZero) return "fail 1"

        if (zero <= floatZero) return "fail 2"

        return "OK"
    }

    return "fail"
}

// 0 Intrinsics\.areEqual
// 0 Double\.compare
// 2 F2D
// 2 DCMPG
// 1 IFNE
// 1 IFGT
