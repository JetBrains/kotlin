fun box(): String {
    val javaClass = JavaClass()

    if (javaClass.isValue != null) return "fail 1"

    javaClass.isValue = false
    if (javaClass.isValue != false) return "fail 2"

    javaClass.isValue = true
    if (javaClass.isValue != true) return "fail 3"

    return "OK"
}