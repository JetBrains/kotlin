// WITH_STDLIB

// Case of KT-44180

fun foo(
    p0: UByte = 1u,
    p1: UShort = 1u,
    p2: UInt = 1u,
    p4: ULong = 1uL,
): ULong {
    return p0.toULong() + p1.toUShort() + p2.toUInt() + p4
}

fun box(): String {
    if (foo() != 4uL) return "Fail 1"
    if (foo(p2 = 10u) != 13uL) return "Fail 2"
    if (foo(10u, 10u, 10u, 10uL) != 40uL) return "Fail 3"

    return "OK"
}