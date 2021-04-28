// !LANGUAGE: +ProperIeee754Comparisons
fun box(): String {
    val zero: Any = 0.0
    val floatZero: Any = -0.0F
    if (zero is Double && floatZero is Float) {
        // TODO: FE1.0 allows comparison of incompatible type after smart cast (KT-46383) but FIR rejects it. We need to figure out a transition plan.
        // if (zero == floatZero) return "fail 1"

        if (zero <= floatZero) return "fail 2"

        return "OK"
    }

    return "fail"
}

// 0 Intrinsics\.areEqual
// 0 Double\.compare
// 1 F2D
// 1 DCMPG
// 0 IFNE
// 1 IFGT
