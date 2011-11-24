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
    System.out?.println(d?.s)
    System.out?.println(d?.s == "s") //prints true
    System.out?.println(d)    //ok
}

fun t4() {
    val e: E? = E()
    System.out?.println(e?.bar() == e) //verify error
    System.out?.println(e?.foo())  //verify error
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
