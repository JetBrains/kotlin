package runtime.workers.freeze5

import kotlin.test.*
object Keys {
    internal val myMap: Map<String, List<String>> = mapOf(
            "val1" to listOf("a1", "a2", "a3"),
            "val2" to listOf("b1", "b2")
    )

    fun getKey(name: String): String {
        for (key in myMap.keys) {
            if (key == name) {
                return key
            }
        }
        return ""
    }

    fun getValue(name: String): String {
        for (value in myMap.values) {
            if (value.contains(name)) {
                return name
            }
        }
        return ""
    }

    fun getEntry(name: String): String {
        for (entry in myMap.entries) {
            if (entry.key == name) {
                return entry.key
            }
        }
        return ""
    }
}
@Test fun runTest() {
    assertEquals("val2", Keys.getKey("val2"))
    assertEquals("a1", Keys.getValue("a1"))
    assertEquals("val1", Keys.getEntry("val1"))
    println("OK")
}