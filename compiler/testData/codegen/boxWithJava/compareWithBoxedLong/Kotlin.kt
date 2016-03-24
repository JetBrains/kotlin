import JavaClass

fun box(): String {
    return if (JavaClass.get() > 0) "OK" else "fail"
}