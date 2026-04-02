// one.A

package one

interface I {
    fun foo() = 4
    fun bar(): Int = 42
}

class A(
    private val p: I
) : I by p