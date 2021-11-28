// IGNORE_BACKEND_FIR: JVM_IR
// FULL_JDK

import java.util.concurrent.*

class Test1 : Iterable<String> {
    private val received = ConcurrentSkipListSet<String>()
    override fun iterator() = received.iterator()
}

class Test2 : Iterable<String> {
    private val received = Array<String>(0) { "" }
    override fun iterator() = received.iterator()
}