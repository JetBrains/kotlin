
fun test(b: Boolean): String {
    if (b) {
        fun result() = "OK"
        return result()
    } else {
        fun result() = "Fail"
        return result()
    }
}
fun box(): String = test(true)
