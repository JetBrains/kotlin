fun box(): String {
    val javaClass = JavaClass()

    if (javaClass.isValue != false) return "fail 1"

    javaClass.isValue = true

    if (javaClass.isValue != true) return "fail 2"

    return "OK"
}