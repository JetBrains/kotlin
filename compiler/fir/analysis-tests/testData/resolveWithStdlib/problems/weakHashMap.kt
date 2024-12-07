// RUN_PIPELINE_TILL: BACKEND
// FULL_JDK

import java.util.*

val someMap = WeakHashMap<Any?, Any?>()

fun foo() {
    someMap[""]
}

