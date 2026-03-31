// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL

interface I {
    val z: Int

    val a: String get() = "test"
}

interface I1 : I {
    override val a: String get() = "test1"
}

open class C(open val x: Int, override val z: Int) : I {
    open val y: String = "a"
    override val a: String = y
}

open class C1(override val y: String, x: Int) : C(1, x) {
    val w = 3
}

object A : C(1, 2) {
    override val y: String = x.toString()
}
