// DUMP_IR
// IGNORE_CODEGEN_WITH_IR_FAKE_OVERRIDE_GENERATION: KT-61386

fun <T> select(a: T, b: T) : T = a

interface A
interface B {
    fun foo(): String
}
class C : A, B {
    override fun foo() = "OK"
}
class D : A, B {
    override fun foo() = "FAIL"
}

fun test(c: C, d: D): String {
    val intersection = select(c, d)
    return object: B by intersection {}.foo()
}

fun box() = test(C(), D())