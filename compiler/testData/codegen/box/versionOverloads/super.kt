// TARGET_BACKEND: JVM
// CHECK_BYTECODE_LISTING
// WITH_STDLIB
@file:OptIn(ExperimentalVersionOverloading::class)

open class A(
    val a: Int = 1,
    @IntroducedAt("1") val b: String = "A1",
    @IntroducedAt("2") val c: Float = 3f,
)

class B : A {
    constructor(a: Int, @IntroducedAt("1") b: String = "B1") : super(a, b)
    constructor(@IntroducedAt("1") b: String = "B2") : super(2, b)
    constructor(b: Boolean) : super(3)
}

class C (
    a : Int,
    @IntroducedAt("1") b: String = "C1",
) : A(a, b) {
    constructor(@IntroducedAt("1") b: String = "C2") : this(2, b)
    constructor(b: Boolean) : this(3)
}

fun box(): String {
    val a1 = A()
    val a2 = A(2)
    val a3 = A(2, "MOO")

    if (a1.b != "A1") return "FAIL-A1"
    if (a2.b != "A1") return "FAIL-A2"
    if (a3.b != "MOO") return "FAIL-A3"

    val b1 = B()
    val b2 = B(2)
    val b3 = B(2, "MOO")
    val b4 = B("MOO")
    val b5 = B(true)

    if (b1.b != "B2") return "FAIL-B1"
    if (b2.b != "B1") return "FAIL-B2"
    if (b3.b != "MOO") return "FAIL-B3"
    if (b4.b != "MOO") return "FAIL-B4"
    if (b5.b != "A1") return "FAIL-B5"

    val c1 = C()
    val c2 = C(2)
    val c3 = C(2, "MOO")
    val c4 = C("MOO")
    val c5 = C(true)

    if (c1.b != "C2") return "FAIL-C1"
    if (c2.b != "C1") return "FAIL-C2"
    if (c3.b != "MOO") return "FAIL-C3"
    if (c4.b != "MOO") return "FAIL-C4"
    if (c5.b != "C1") return "FAIL-C5"

    return "OK"
}
