// LANGUAGE: +DataFlowBasedExhaustiveness
// IGNORE_BACKEND_K1: ANY

sealed class Variants {
    object A : Variants()
    object B : Variants()
    object C : Variants()
}

fun simpleSealed(v: Variants): String {
        if (v is Variants.A) {
            return "A"
        }
        return when (v) {
            Variants.B -> "B"
            Variants.C -> "C"
        }
}


fun simpleSealed2(v: Variants): String {
    if (v is Variants.A) {
        return "A"
    }
    if (v is Variants.B) {
        return "B"
    }
    return when (v) {
        Variants.C -> "C"
    }
}

fun simpleSealedThrow(x: Variants): Int {
    (x is Variants.A) && throw IllegalArgumentException("A")
    return when (x) {
        Variants.B -> 2
        Variants.C -> 3
    }
}



fun box(): String {
    val a = Variants.A
    val b = Variants.B
    val c = Variants.C

    if (simpleSealed(a) != "A") return "Fail1"
    if (simpleSealed(b) != "B") return "Fail2"
    if (simpleSealed(c) != "C") return "Fail3"

    if (simpleSealed2(a) != "A") return "Fail4"
    if (simpleSealed2(b) != "B") return "Fail5"
    if (simpleSealed2(c) != "C") return "Fail6"

    try {
        simpleSealedThrow(a)
        return "Fail7"
    } catch (e: IllegalArgumentException) {
        if (e.message != "A") return "Fail8"
    }
    if (simpleSealedThrow(b) != 2) return "Fail9"
    if (simpleSealedThrow(c) != 3) return "Fail10"

    return "OK"
}