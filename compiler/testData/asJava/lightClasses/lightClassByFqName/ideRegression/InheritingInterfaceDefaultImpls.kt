// p.Inheritor
package p

annotation class Anno(vararg val s: String)

annotation class Bueno(val anno: Anno)

class Inheritor: I, I2 {

    fun f() {

    }

    override fun g() {
    }
}

interface I : I1 {
    fun g()
}

interface I1 {
    @Bueno(Anno("G"))
    fun foo() = "foo"
}

interface I2 {
    @Anno("S")
    fun bar() = "bar"
}

// FIR_COMPARISON