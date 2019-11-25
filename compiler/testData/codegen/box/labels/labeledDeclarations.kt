// IGNORE_BACKEND_FIR: JVM_IR
data class A(val a: Int, val b: Int)

fun box() : String
{
    a@ val x = 1
    b@ fun a() = 2
    c@ val (z, z2) = A(1, 2)

    if (x != 1) return "fail 1"

    if (a() != 2) return "fail 2"

    if (z != 1 || z2 != 2) return "fail 3"

    return "OK"
}