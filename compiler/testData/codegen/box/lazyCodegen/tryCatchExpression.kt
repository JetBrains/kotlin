class A {
    val p : Int = try{
        1
    } catch(e: Exception) {
        throw RuntimeException()
    }
}

fun box() : String {
    if (A().p != 1) return "fail 1"

    return "OK"
}