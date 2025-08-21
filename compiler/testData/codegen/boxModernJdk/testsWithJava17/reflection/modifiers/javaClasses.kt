// WITH_REFLECT

// FILE: Record.java
public record Record(String value) {}

// FILE: box.kt

import kotlin.test.assertTrue
import kotlin.test.assertFalse

fun box(): String {
    assertFalse(Record::class.isData)
    assertFalse(Record::class.isInner)
    assertFalse(Record::class.isCompanion)
    assertFalse(Record::class.isFun)
    assertFalse(Record::class.isValue)

    return "OK"
}
