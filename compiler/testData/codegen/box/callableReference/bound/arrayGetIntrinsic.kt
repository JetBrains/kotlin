
fun box(): String {
    return if ((arrayOf(1, 2, 3)::get).let { it(1) } == 2) "OK" else "Fail"
}
