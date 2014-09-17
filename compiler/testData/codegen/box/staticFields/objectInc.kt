object A {
    private var r: Int = 1;

    fun a() : Int {
        r++
        ++r
        return r
    }
}

fun box() : String {
    val p = A.a()
    return if (p == 3) return "OK" else "fail"
}