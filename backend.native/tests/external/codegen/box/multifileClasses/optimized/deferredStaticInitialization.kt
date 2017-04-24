// TARGET_BACKEND: JVM
// IGNORE_LIGHT_ANALYSIS
// WITH_RUNTIME
// KOTLIN_CONFIGURATION_FLAGS: +JVM.INHERIT_MULTIFILE_PARTS
// FILE: box.kt

import a.*

fun box(): String = OK

// FILE: part1.kt
@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

val O = run { "O" }

// FILE: part2.kt
@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

const val K = "K"

// FILE: part3.kt
@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

val OK: String = run { O + K }

// FILE: irrelevantPart.kt
@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

val X1: Nothing =
        throw AssertionError("X1 should not be initialized")

// FILE: reallyIrrelevantPart.kt
@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

val X2: Nothing =
        throw AssertionError("X2 should not be initialized")
