class T {
    class U {
        open class A
    }
}

fun test() {
    val b = object : B() {
        override fun bar(s: String) = s.length()
    }
    val t1 = b.x
    b.x = t1 + 1
    val t2 = B.X
    b.foo(1)
    B.foo2(2)
    B.Y()
}