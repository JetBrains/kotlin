// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

import java.util.HashSet

fun box(): String {
    val a = HashSet<String>()
    a.add("live")
    a.add("long")
    a.add("prosper")
    val b = a.clone()
    if (a != b) return "Fail equals"
    if (a === b) return "Fail identity"
    return "OK"
}
