// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
//FILE:a.kt
package a.foo

//FILE:b.kt
package a

fun foo() = 2

//FILE:c.kt
package c

import a.foo
