class Usages {
    fun foo(a: A) {
        a.foo("")
        println("${a.a} ${a.b}")
        a.b = 12
    }

    fun foo(x: X) {
        x.foo("")
        println("${x.a} ${x.b}")
        x.b = 12
    }
}