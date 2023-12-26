// TARGET_BACKEND: JVM_IR
// ISSUE: KT-54654
// JVM_ABI_K1_K2_DIFF: KT-63828, KT-63871

fun accessProperty(b: B) = b.property
fun accessFunction(b: B) = b.function()

fun getString_1(): String = "O"
fun getString_2(): String = "K"

interface A {
    val property get() = getString_1()
    fun function() = getString_2()
}

class B(val a: A) : A by a

class C : A

fun box(): String {
    val b = B(C())
    return accessProperty(b) + accessFunction(b)
}
