// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    val a = 1
    val explicitlyReturned = run1 f@{
        if (a > 0)
          return@f "OK"
        else "Fail 1"
    }
    if (explicitlyReturned != "OK") return explicitlyReturned

    val implicitlyReturned = run1 f@{
        if (a < 0)
          return@f "Fail 2"
        else "OK"
    }
    if (implicitlyReturned != "OK") return implicitlyReturned
    return "OK"
}

fun <T> run1(f: () -> T): T { return f() }