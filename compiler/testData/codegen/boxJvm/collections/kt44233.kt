// TARGET_BACKEND: JVM
// FULL_JDK

import java.util.concurrent.ConcurrentSkipListSet

class StringIterable : Iterable<String> {
    private val strings = ConcurrentSkipListSet<String>()
    override fun iterator() = strings.iterator()
}

fun box(): String {
    val si = StringIterable()
    return if (si.iterator().hasNext())
        "Failed"
    else
        "OK"
}
