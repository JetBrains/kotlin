import test.*

fun box() : String {
    val call = call(11, ::A)
    return if (call == 11) "OK" else "fail"
}

