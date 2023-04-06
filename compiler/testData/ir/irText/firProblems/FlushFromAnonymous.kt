// TARGET_BACKEND: JVM_IR
// DUMP_LOCAL_DECLARATION_SIGNATURES
// SKIP_KLIB_TEST

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57430

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
