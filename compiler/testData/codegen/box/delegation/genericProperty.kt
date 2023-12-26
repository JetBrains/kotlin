// JVM_ABI_K1_K2_DIFF: KT-63828, KT-63855, KT-63871

interface I {
    val <T> T.id: T
        get() = this
}

class A(i: I) : I by i

fun box(): String = with(A(object : I {})) { "OK".id }
