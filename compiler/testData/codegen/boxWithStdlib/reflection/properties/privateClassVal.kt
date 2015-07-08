import kotlin.reflect.*
import kotlin.reflect.jvm.accessible

class Result {
    private val value = "OK"

    fun ref() = Result::class.properties.single() as KProperty1<Result, String>
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
