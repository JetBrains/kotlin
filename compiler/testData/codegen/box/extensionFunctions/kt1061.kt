// IGNORE_BACKEND_FIR: JVM_IR
//KT-1061 Can't call function defined as a val

object X {
    val doit = { i: Int -> i }
}

fun box() : String = if (X.doit(3) == 3) "OK" else "fail"
