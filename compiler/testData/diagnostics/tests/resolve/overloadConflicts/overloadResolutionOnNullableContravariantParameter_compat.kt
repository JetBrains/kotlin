// !WITH_NEW_INFERENCE
// !CONSTRAINT_SYSTEM_FOR_OVERLOAD_RESOLUTION: CONSTRAINT_SYSTEM_FOR_OLD_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: a.kt

package a

fun <T> foo(block: (T?) -> Unit) {}

// FILE: b.kt

package b

fun <K> foo(block: (K) -> Unit) {}

// FILE: test.kt

import a.*
import b.*

fun main() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!><String> {  }
}