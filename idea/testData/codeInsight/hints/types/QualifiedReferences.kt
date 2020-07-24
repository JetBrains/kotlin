// MODE: local_variable
package p
class A {
    class B {
        class C {
            class D
        }
    }
    inner class E
    enum class F { enumCase }
}
fun foo() {
    val v1 = A.B.C.D()
    val v2 = p.A.B.C.D()
    val v3<# : A.E #> = A().E()
    val v4 = p.A.F.enumCase
    val v5 = A.F.enumCase
    val v6 = p.A()


}

