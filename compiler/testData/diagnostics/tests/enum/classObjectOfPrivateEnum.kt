// FILE: 1.kt
package a

private enum class E { ENTRY }

// FILE: 2.kt
package b

val e = a.<!INVISIBLE_MEMBER!>E<!>
