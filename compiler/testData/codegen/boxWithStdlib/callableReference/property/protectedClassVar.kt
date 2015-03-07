import kotlin.reflect.IllegalPropertyAccessException
import kotlin.reflect.jvm.accessible

class A(param: String) {
    protected var v: String = param

    fun ref() = ::v
}

fun box(): String {
    val a = A(":(")
    val f = a.ref()

    try {
        f.get(a)
        return "Fail: protected property getter is accessible by default"
    } catch (e: IllegalPropertyAccessException) { }

    try {
        f.set(a, ":D")
        return "Fail: protected property setter is accessible by default"
    } catch (e: IllegalPropertyAccessException) { }

    f.accessible = true

    f.set(a, ":)")

    return if (f[a] != ":)") "Fail: ${f[a]}" else "OK"
}
