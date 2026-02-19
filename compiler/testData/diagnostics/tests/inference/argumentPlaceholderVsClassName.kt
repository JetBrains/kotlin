// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

fun main() {
    var x = listOf<`_`>(A)
    x = listOf(object : `_` {})
}

interface `_`
object A : `_`

/* GENERATED_FIR_TAGS: anonymousObjectExpression, assignment, functionDeclaration, interfaceDeclaration, localProperty,
objectDeclaration, propertyDeclaration */
