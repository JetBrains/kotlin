// FIR_DUMP

class A<in T>(t: T) {
    private val t: T = t  // PRIVATE_TO_THIS

    fun test() {
        val x: T = t      // Ok
        val y: T = this.t // Ok
    }

    fun foo(a: A<String>) {
        val x: String = a.t // Invisible!
    }
}