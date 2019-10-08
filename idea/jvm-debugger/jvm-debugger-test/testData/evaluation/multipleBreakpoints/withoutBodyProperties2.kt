package withoutBodyProperties2

import kotlin.properties.Delegates

// EXPRESSION: 1 + 4
// RESULT: 5: I
//FieldWatchpoint! (aWoBody)
val aWoBody: Int get() = 1

// EXPRESSION: 1 + 5
// RESULT: 6: I
//FieldWatchpoint! (aWoBody2)
val aWoBody2: Int get() { return 1 }

fun main(args: Array<String>) {
    aWoBody
    aWoBody2
}

// WATCH_FIELD_ACCESS: true
// WATCH_FIELD_MODIFICATION: false