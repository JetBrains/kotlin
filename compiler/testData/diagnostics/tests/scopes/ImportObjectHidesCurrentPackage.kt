//FILE:a.kt
package a

// no error is reported
import b.a

fun foo() = a

//FILE:b.kt
package b

object a {}
