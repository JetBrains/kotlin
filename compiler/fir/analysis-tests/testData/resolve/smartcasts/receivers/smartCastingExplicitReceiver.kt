// !DUMP_CFG
interface I

class C : I {
    fun I.foo() = "ret"
}

fun I.bar() {
    (this as C).foo()
}
