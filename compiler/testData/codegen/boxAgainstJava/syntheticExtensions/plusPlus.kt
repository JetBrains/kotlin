fun box(): String {
    val javaClass = JavaClass()
    javaClass.x++
    return if (javaClass.x == 1) "OK" else "ERROR"
}
