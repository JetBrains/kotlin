class X(val ok: String) {
    fun y(): String = ok
}

fun box(): String {
    val x = X("OK")
    val y = x::y
    return y()
}

//fun y(): String = "OK"
//
//fun box(): String {
//    val y = ::y
//    return y.invoke()
//}

//val x = "OK"
//
//fun box(): String {
//    val x = ::x
//    return x.get()
//}