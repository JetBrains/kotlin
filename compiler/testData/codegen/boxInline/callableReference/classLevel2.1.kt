import test.*

fun box() : String {
    val call = A(11).test()
    return if (call == 11) "OK" else "fail"
}

