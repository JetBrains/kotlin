// MODULE: lib
// MODULE_KIND: LibrarySource
// FILE: a.kt
package test

private val pv: String = ""

// FILE: b.kt
package test

private val pv: Int = 1

fun usage() = p<caret>v
