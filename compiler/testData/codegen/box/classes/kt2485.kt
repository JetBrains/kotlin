fun f1(a : Any?) {}
fun f2(a : Boolean?) {}
fun f3(a : Any) {}
fun f4(a : Boolean) {}

fun box() : String {
    f1(1 == 1)
    f2(1 == 1)
    f3(1 == 1)
    f4(1 == 1)
    return "OK"
}
