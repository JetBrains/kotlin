// IGNORE_BACKEND_FIR: JVM_IR
//KT-3276

fun box(): String {
    fun rec(n : Int) {
        val x = { m : Int ->
            if (n > 0) rec(n - 1)
        }

        x(0)
    }

    rec(5)
    
    return "OK"
}
