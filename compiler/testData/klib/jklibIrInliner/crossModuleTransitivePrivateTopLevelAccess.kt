// MODULE: lib
// FILE: lib.kt
package lib

private fun privateTopLevelFunction(): Int = 1

@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
internal inline fun libInlineFunction(): Int = privateTopLevelFunction()

// MODULE: middle()(lib)
// FILE: middle.kt
package middle

import lib.libInlineFunction

@Suppress("NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE")
inline fun middleInlineFunction(): Int = libInlineFunction()

// MODULE: main(lib, middle)
// FILE: main.kt
package test

import middle.middleInlineFunction

fun test(): Int = middleInlineFunction()
