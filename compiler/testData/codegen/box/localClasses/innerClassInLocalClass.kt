// IGNORE_BACKEND_FIR: JVM_IR
class A {
    val a = 1
    fun calc () : Int {
        class B() {
            val b = 2
            inner class C {
                val c = 3
                fun calc() = this@A.a + this@B.b + this.c
            }
        }
        return B().C().calc()
    }
}

fun box() : String {
    return if (A().calc() == 6) "OK" else "fail" 
}