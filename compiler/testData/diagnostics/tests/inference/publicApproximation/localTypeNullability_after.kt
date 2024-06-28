// ISSUE: KT-53982
// FIR_IDENTICAL
// LANGUAGE: +KeepNullabilityWhenApproximatingLocalType

interface I

val condition = false

fun foo4()  = if (condition) object : I {} else null

fun bar1() = foo4().toString()

fun bar2() = foo4()?.toString()

object J {
    fun <T> id(x: T): T? {
        return null
    }
}

fun foo5() = J.id(object : I {})

fun bar3() = foo5()?.toString()