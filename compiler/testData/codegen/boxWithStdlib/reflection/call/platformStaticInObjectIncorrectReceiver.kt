import kotlin.jvm.JvmStatic as static

object Obj {
    @static fun foo(s: String) {}
    @static fun bar() {}
    @static fun sly(obj: Obj) {}

    operator fun get(name: String) = Obj::class.members.single { it.name == name }
}

fun box(): String {
    // This should succeed
    (Obj["foo"]).call(Obj, "")
    (Obj["bar"]).call(Obj)
    (Obj["sly"]).call(Obj, Obj)

    // This shouldn't: first argument should always be Obj
    try {
        (Obj["foo"]).call(null, "")
        return "Fail foo"
    } catch (e: IllegalArgumentException) {}

    try {
        (Obj["bar"]).call("")
        return "Fail bar"
    } catch (e: IllegalArgumentException) {}

    try {
        (Obj["sly"]).call(Obj)
        return "Fail sly 1"
    } catch (e: IllegalArgumentException) {}

    try {
        (Obj["sly"]).call(null, Obj)
        return "Fail sly 2"
    } catch (e: IllegalArgumentException) {}

    return "OK"
}
