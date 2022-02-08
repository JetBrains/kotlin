// !LANGUAGE: +NewInference -StrictOnlyInputTypesChecks
// !DIAGNOSTICS: -UNUSED_PARAMETER

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <@kotlin.internal.OnlyInputTypes S> select(a1: S, a2: S): S = TODO()

interface Common
class First : Common
class Second : Common

fun test(first: First, second: Second) {
    select(first, second)
}
