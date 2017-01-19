// !DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: a.kt
package a

interface A

operator fun A.plus(other: A): A = this

// FILE: b.kt
package b

import a.A
import a.<!OPERATOR_RENAMED_ON_IMPORT!>plus<!> as minus

fun test(a1: A, a2: A) =
        a1 - a2