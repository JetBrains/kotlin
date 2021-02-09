// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR

class X<T: Number>(val y: Any, val x: T)

fun box(): String {
    val num: Long = -10
    val num2: Int = 20
    val obj = if (true)
        X(Any(), if (true) num else num2)
    else
        X(Any(), -25)
    val f = obj.y
    return "OK"
}
