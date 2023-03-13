// SKIP_KLIB_TEST
// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6
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
