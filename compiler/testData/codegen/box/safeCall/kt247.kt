// IGNORE_BACKEND_FIR: JVM_IR
fun t1() : Boolean {
    val s1 : String? = "sff"
    val s2 : String? = null
    return s1?.length == 3 && s2?.length == null
}

fun t2() : Boolean {
    val c1: C? = C(1)
    val c2: C? = null
    return c1?.x == 1 && c2?.x == null
}

fun t3() {
    val d: D = D("s")
    val x = d?.s
    if (!(d?.s == "s")) throw AssertionError()
}

fun t4() {
    val e: E? = E()
    if (!(e?.bar() == e)) throw AssertionError()
    val x = e?.foo()
}

fun box() : String {
    if(!t1 ()) return "fail"
    if(!t2 ()) return "fail"
    t3()
    t4()
    return "OK"
}

class C(val x: Int)
class D(val s: String)
class E() {
    fun foo() = 1
    fun bar() = this
}
