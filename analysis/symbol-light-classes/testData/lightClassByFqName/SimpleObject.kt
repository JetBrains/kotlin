// pack.A

package pack

object A {
    val c = { 1 }()
    var v = { "A" }()
    fun f() = 3

    const val cc = 1
    const val cv = "A"
}
// LIGHT_ELEMENTS_NO_DECLARATION: A.class[A;INSTANCE]