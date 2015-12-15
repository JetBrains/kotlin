fun box(): String {
    val sub = Box<Long>(-1)
    return if (sub.value == -1L) "OK" else "fail"
}