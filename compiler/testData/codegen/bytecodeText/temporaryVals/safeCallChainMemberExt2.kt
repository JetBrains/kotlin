import Host.b
import Host.c
import Host.s

class A
class B
class C

object Host {
    val A.b: B get() = B()
    val B.c: C get() = C()
    val C.s: String get() = "s"
}

fun test(an: A?) = an?.b?.c?.s

// JVM_IR_TEMPLATES
// 0 ASTORE
// 1 ACONST_NULL
// 3 IFNULL
// 0 IFNONNULL