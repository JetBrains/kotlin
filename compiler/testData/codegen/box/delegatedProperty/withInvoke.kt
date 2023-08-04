// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// ISSUE: KT-61633

class C<T3>(val p: T3)
class D<T4>(val p: T4)

val <X3> C<X3>.getValue: D<X3> get() = D(p)
operator fun <X4> D<X4>.invoke(x: Any?, y: Any?): X4 = p

fun foo(c: C<String>): String {
    val y1 by c

    return y1
}

fun box(): String = foo(C("OK"))