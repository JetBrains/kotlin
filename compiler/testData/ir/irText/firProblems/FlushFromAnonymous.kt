// FIR_IDENTICAL
// TARGET_BACKEND: JVM_IR

// FILE: Collector.java

public class Collector {
    public void flush() {}
}

// FILE: FlushFromAnonymous.kt

class Serializer() {
    fun serialize() {
        val messageCollector = createMessageCollector()
        try {

        } catch (e: Throwable) {
            messageCollector.flush()
        }
    }

    private fun createMessageCollector() = object : Collector() {}
}
