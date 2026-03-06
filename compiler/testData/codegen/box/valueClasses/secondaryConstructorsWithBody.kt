// LANGUAGE: +ValueClasses, +ValueClassesSecondaryConstructorWithBody
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING
// WITH_STDLIB
// FIR_IDENTICAL

val l = mutableListOf<Any>()

@JvmInline
value class VC(val x: Int, val y: ULong) {
    constructor(xD: Double, yD: Double) : this(xD.toInt() - 2, yD.toULong() - 2UL) {
        l.add(xD)
        l.add(yD)
        l.add(x)
        l.add(y)
        l.add(this)
        l.add(xD.let { it - 1.0 }.let(fun(x: Double) = x - 1.0))
        class Inner(val x: Int) {
            constructor(x: Long) : this(x.toInt()) {
                l.add(x)
            }
        }
        Inner(Long.MAX_VALUE)
    }

    init {
        l.add(x)
        l.add(y)
        l.add(this)
    }
}

fun box(): String {
    val vc = VC(1, 2UL)
    require(vc == VC(3.0, 4.0)) { "$vc\n${VC(3.0, 4.0)}" }
    val actual = listOf(1, 2UL, vc, 1, 2UL, vc, 3.0, 4.0, 1, 2UL, vc, 1.0, Long.MAX_VALUE)
    require(l == actual) { "$l\n$actual" }
    return "OK"
}
