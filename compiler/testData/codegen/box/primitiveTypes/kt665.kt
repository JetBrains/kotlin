// IGNORE_BACKEND_FIR: JVM_IR
fun f(x: Long, zzz: Long = 1): Long
{
    return if (x <= 1) zzz
    else f(x-1, x*zzz)
}

fun box() : String
{
    val six: Long = 6;
    if (f(six) != 720.toLong()) return "Fail"
    return "OK"
}
