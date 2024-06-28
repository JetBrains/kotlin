// WITH_STDLIB
// ISSUE: KT-49696

interface Listener {
    fun added(item: Any)
    fun removed(item: Any)
}

val listeners = mutableListOf<Listener>()

fun smartCast(item: Any?) {
    var current = item
    if (current == null) {
        current = Any()
        listeners.forEach { it.added(<!SMARTCAST_IMPOSSIBLE!>current<!>) }
    } else {
        listeners.forEach { it.removed(<!SMARTCAST_IMPOSSIBLE!>current<!>) }
        current = Any()
        listeners.forEach { it.added(<!DEBUG_INFO_SMARTCAST!>current<!>) }
    }
}
