// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    fun rec(n : Int) {
        fun x(m : Int, k : Int) {
            if (n > 0) rec(n - 1 + k)
        }

        x(0, 0)
    }

    rec(5)
    
    return "OK"
}
