// !DUMP_CFG

abstract class A(func: () -> String)

class B(val s: String) : A(s.let { { it } }) {
    fun foo() {
        foo()
    }
}
