// LANGUAGE: +DataFlowBasedExhaustiveness
// IGNORE_BACKEND_K1: ANY

enum class Enum { A, B, C }

fun foo(e: Enum): Int {
    if (e == Enum.A) return 1
    return when (e) {
        Enum.B -> 2
        Enum.C -> 3
    }
}

fun bar(e: Enum): Int {
    if (e == Enum.A) return 1
    if (e == Enum.B) return 2
    return when (e) {
        Enum.C -> 3
    }
}

fun simpleEnum(x: Enum): Int {
    if (x == Enum.C) return 1
    return when (x) {
        Enum.A, Enum.B -> 3
    }
}

fun simpleEnumThrow(x: Enum): Int {
    if (x == Enum.C) throw AssertionError("C")
    return when (x) {
        Enum.A -> 2
        Enum.B -> 3
    }
}

fun simpleEnumThrow2(x: Enum): Int {
    if (x == Enum.A) throw IllegalArgumentException("A")
    if (x == Enum.B) throw IllegalArgumentException("B")
    return when (x) {
        Enum.C -> 3
    }
}

fun box(): String {
    if (foo(Enum.A) != 1) return "Fail1"
    if (foo(Enum.B) != 2) return "Fail2"
    if (foo(Enum.C) != 3) return "Fail3"

    if (bar(Enum.A) != 1) return "Fail4"
    if (bar(Enum.B) != 2) return "Fail5"
    if (bar(Enum.C) != 3) return "Fail6"

    if (simpleEnum(Enum.C) != 1) return "Fail7"
    if (simpleEnum(Enum.A) != 3) return "Fail8"
    if (simpleEnum(Enum.B) != 3) return "Fail9"

    try {
        simpleEnumThrow(Enum.C)
        return "Fail10"
    } catch (e: AssertionError) {
        if (e.message != "C") return "Fail11"
    }
    if (simpleEnumThrow(Enum.A) != 2) return "Fail12"
    if (simpleEnumThrow(Enum.B) != 3) return "Fail13"

    try {
        simpleEnumThrow2(Enum.A)
        return "Fail14"
    } catch (e: IllegalArgumentException) {
        if (e.message != "A") return "Fail15"
    }
    try {
        simpleEnumThrow2(Enum.B)
        return "Fail16"
    } catch (e: IllegalArgumentException) {
        if (e.message != "B") return "Fail17"
    }
    if (simpleEnumThrow2(Enum.C) != 3) return "Fail18"

    return "OK"
}