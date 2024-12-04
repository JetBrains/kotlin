class A {
    override fun toString(): String = "A"
    inner class B {
        override fun toString(): String = "B"
        inner class C(val a: A = this@A, val b: B = this@B) {
            override fun toString(): String = "C${f1()}${f2()}${f3()}${f4()}${f5()}"
            fun f1(): String = "C"
            fun f2(): String = a.toString()
            fun f3(): String = b.toString()
            fun f4(): String = this@A.toString()
            fun f5(): String = this@B.toString()
            fun f6(aa: A = this@A): String = aa.toString()
        }
    }
}


fun box(): String {
    val c = A().B().C()
    if (c.toString() != "CCABAB") "FAIL1"
    if (c.f1() != "C") return "FAIL2"
    if (c.f2() != "A") return "FAIL3"
    if (c.f3() != "B") return "FAIL4"
    if (c.f4() != "A") return "FAIL5"
    if (c.f5() != "B") return "FAIL6"
    if (c.f6() != "A") return "FAIL7"
    return "OK"
}
