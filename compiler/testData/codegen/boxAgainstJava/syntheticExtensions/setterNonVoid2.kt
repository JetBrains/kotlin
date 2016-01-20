fun box(): String {
    val javaClass = JavaClass()
    if (javaClass.x.isEmpty()) {
        javaClass.x = "OK"
    }
    return javaClass.x
}
