import kotlin.platform.platformStatic

var holder = ""

fun getA(): A {
    holder += "OK"
    return A
}

object A {
    @platformStatic fun a(): String {
        return holder
    }
}

fun box(): String {
    return getA().a()
}
