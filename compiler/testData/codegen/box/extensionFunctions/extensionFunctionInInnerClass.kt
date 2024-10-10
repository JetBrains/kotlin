class Outer {
    val a = 1
    inner class Nested {
        val a = 2
        inner class Inner {
            val a = 3
            fun Outer.foo(): Int {
                val a = 4
                return this@Inner.a + this@Nested.a + this@Outer.a + this@foo.a + a
            }
        }
    }
}

fun box(): String {
    with(Outer().Nested().Inner()) {
        return if (Outer().foo() == 11) "OK"
        else "FAIL"
    }
}