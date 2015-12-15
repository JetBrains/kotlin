fun box(): String {
    val sub = Box<Long>(-1)
    println(sub.value == 1L)
    return "OK"
}