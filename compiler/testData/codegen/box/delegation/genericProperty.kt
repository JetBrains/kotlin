// JVM_ABI_K1_K2_DIFF: KT-63855

interface I {
    val <T> T.id: T
        get() = this
}

class A(i: I) : I by i

fun box(): String = with(A(object : I {})) { "OK".id }
