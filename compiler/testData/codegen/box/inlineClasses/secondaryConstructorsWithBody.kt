// LANGUAGE: +ValueClassesSecondaryConstructorWithBody
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING
// WITH_STDLIB
// FIR_IDENTICAL

val l = mutableListOf<Any>()

@JvmInline
value class VC(val x: Int) {
    constructor(xD: Double): this(-xD.toInt()) {
        l.add(xD)
        l.add(x)
        l.add(this)
    }
    
    init {
        l.add(x)
        l.add(this)
    }
}

fun box(): String {
    val vc = VC(1)
    require(vc == VC(-1.0)) { "$vc\n${VC(-1.0)}"}
    val actual = listOf(1, vc, 1, vc, -1.0, 1, vc)
    require(l == actual) { "$l\n$actual" }
    return "OK"
}
