var log : String = ""
fun log(a: String) {
    log += a + ";"
}

interface C {
    open  fun foo(x: Int) {
        log("C.foo(${x})")
    }

}
interface I {
    abstract  fun foo(x: Int  = 1)
}
class G(val c: C): C by c, I {
}
class H(val c: C): I, C by c {
}
fun test1() {
    null.log = ""
    val g1 : G = G(object: C {
})
    g1.foo(2)
    g1.foo()
    val g2 : G = G(object: C {
    override fun foo(x: Int) {
        log("[2] object:C.foo(${x})")
    }

})
    g2.foo(2)
    g2.foo()
}

fun test2() {
    null.log = ""
    val h1 : H = H(object: C {
})
    h1.foo(2)
    h1.foo()
    val h2 : H = H(object: C {
    override fun foo(x: Int) {
        log("[2] object:C.foo(${x})")
    }

})
    h2.foo(2)
    h2.foo()
}

fun box() : String  {
    test1()
    if (log != "C.foo(2);C.foo(1);[2] object:C.foo(2);[2] object:C.foo(1);") {
        return "fail1: ${log}"
    }
    test2()
    if (log != "C.foo(2);C.foo(1);[2] object:C.foo(2);[2] object:C.foo(1);") {
        return "fail2: ${log}"
    }
    return "OK"
}
