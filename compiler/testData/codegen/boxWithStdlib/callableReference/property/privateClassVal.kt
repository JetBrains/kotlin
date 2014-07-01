import kotlin.reflect.IllegalAccessException
import kotlin.reflect.KMemberProperty
import kotlin.reflect.jvm.accessible

class Result {
    private val value = "OK"

    fun ref(): KMemberProperty<Result, String> = ::value
}

fun box(): String {
    val p = Result().ref()
    try {
        p.get(Result())
        return "Fail: private property is accessible by default"
    } catch(e: IllegalAccessException) { }

    p.accessible = true

    val r = p.get(Result())

    p.accessible = false
    try {
        p.get(Result())
        return "Fail: setAccessible(false) had no effect"
    } catch(e: IllegalAccessException) { }

    return r
}
