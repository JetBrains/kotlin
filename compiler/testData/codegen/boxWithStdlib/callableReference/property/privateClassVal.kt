import kotlin.reflect.IllegalPropertyAccessException
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.accessible

class Result {
    private val value = "OK"

    fun ref(): KProperty1<Result, String> = ::value
}

fun box(): String {
    val p = Result().ref()
    try {
        p.get(Result())
        return "Fail: private property is accessible by default"
    } catch(e: IllegalPropertyAccessException) { }

    p.accessible = true

    val r = p.get(Result())

    p.accessible = false
    try {
        p.get(Result())
        return "Fail: setAccessible(false) had no effect"
    } catch(e: IllegalPropertyAccessException) { }

    return r
}
