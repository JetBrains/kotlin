// MODULE: lib
// FILE: lib.kt
package lib

private fun privateTopLevelFunction(): Int = 1

class C {
    private fun privateFunction(): Int = 2

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    internal inline fun internalInlineFunction(): Int = privateFunction() + privateTopLevelFunction()
}

// MODULE: main()(lib)
// FILE: main.kt
package test

import lib.C

fun test(): Int = C().internalInlineFunction()
