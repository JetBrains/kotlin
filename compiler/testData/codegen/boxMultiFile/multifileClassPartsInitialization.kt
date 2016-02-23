// FILE: box.kt

import a.*

fun box(): String = OK

// FILE: part1.kt

@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

val O: String = "O"

// FILE: part2.kt

@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

val K: String = "K"

// FILE: part3.kt

@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

val OK: String = O + K
