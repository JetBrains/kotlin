class Usages {
    fun foo(a: A) {
        a.foo("")
        println("${a.a} ${a.b}")
        println("${a.t} ${a.u}")
        a.b = 12
        a.u = 13
    }

    fun foo(x: X) {
        x.foo("")
        println("${x.a} ${x.b}")
        x.b = 12
    }
}