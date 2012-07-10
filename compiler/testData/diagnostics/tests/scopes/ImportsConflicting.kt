//FILE:a.kt
package a

import b.foo
import c.foo // TODO: need warning here

//FILE:b.kt
package b

fun foo() = 2

//FILE:c.kt
package c

fun foo() = 1
