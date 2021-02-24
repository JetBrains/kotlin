// WITH_REFLECT
// KJS_WITH_FULL_RUNTIME

import kotlin.reflect.*

val properties = HashSet<KProperty<*>>()

object Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): String {
        properties.add(p)
        return ""
    }

    operator fun setValue(t: Any?, p: KProperty<*>, v: String) {
        properties.add(p)
    }
}

var topLevel: String by Delegate
object O {
    var member: String by Delegate
    var O.memExt: String by Delegate
}

fun box(): String {
    topLevel = ""
    O.member = ""
    with (O) {
        O.memExt = ""
    }

    for (p in HashSet(properties)) {
        // None of these should fail

        (p as? KProperty0<String>)?.get()
        (p as? KProperty1<O, String>)?.get(O)
        (p as? KProperty2<O, O, String>)?.get(O, O)

        (p as? KMutableProperty0<String>)?.set("")
        (p as? KMutableProperty1<O, String>)?.set(O, "")
        (p as? KMutableProperty2<O, O, String>)?.set(O, O, "")
    }

    return "OK"
}
