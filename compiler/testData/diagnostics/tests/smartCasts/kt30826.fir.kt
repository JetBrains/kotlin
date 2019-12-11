// !WITH_NEW_INFERENCE
// Issue: KT-30826

interface I1
interface I2 {
    fun foo() {}
}

class A : I1, I2

fun foo(x: I1?) {
    var y = x
    y as I2
    val bar = {
        y.foo() // NPE in NI, smartcast impossible in OI
    }
    y = null
    bar()
}