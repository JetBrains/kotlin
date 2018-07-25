// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.jvm.*

object Obj {
    @JvmStatic
    private var result: String = "Fail"
}

fun box(): String {
    val p = Obj::class.members.single { it.name == "result" } as KMutableProperty1<Any?, String>
    p.isAccessible = true

    try {
        p.set(null, "OK")
        return "Fail: set should check that first argument is Obj"
    } catch (e: IllegalArgumentException) {}

    try {
        p.get(null)
        return "Fail: get should check that first argument is Obj"
    } catch (e: IllegalArgumentException) {}

    p.set(Obj, "OK")
    return p.get(Obj)
}
