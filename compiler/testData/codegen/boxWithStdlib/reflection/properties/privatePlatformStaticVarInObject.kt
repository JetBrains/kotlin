import kotlin.reflect.*
import kotlin.reflect.jvm.*
import kotlin.platform.platformStatic as static

object Obj {
    private static var result: String = "Fail"
}

fun box(): String {
    val p = Obj::class.members.single { it.name == "result" } as KMutableProperty1<Any?, String>
    p.isAccessible = true

    try {
        p[null] = "OK"
        return "Fail: set should check that first argument is Obj"
    } catch (e: IllegalArgumentException) {}

    try {
        p[null]
        return "Fail: get should check that first argument is Obj"
    } catch (e: IllegalArgumentException) {}

    p[Obj] = "OK"
    return p[Obj]
}
