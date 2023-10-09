// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// JVM_ABI_K1_K2_DIFF: KT-62464

open class A(val x: String) {
    inline fun f() = if (this is C) this else A("O")

    val y
        inline get() = if (this is C) this else A("K")
}

class B : A("unused")
class C : A("unused")

// If the receiver is not CHECKCASTed to A when inlining, asm will infer Object
// for the result of `if` in `f` instead of A when generating stack maps because
// one branch has type A while the other has type B (a subtype of A, but asm
// does not know that). This would cause a JVM validation error.
fun box() = B().f().x + B().y.x
