import test.*

fun box() : String {
    val call = call(A(11), A::calc)
    return if (call == 11) "OK" else "fail"
}

