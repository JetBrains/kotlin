// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-49722

interface I {
    fun h(x: Int = 3, block: () -> String): Any
}

open class A : I {
    // [NOT_YET_SUPPORTED_IN_INLINE] Functional parameters with inherited default values are not yet supported in inline functions.
    final override inline <!OVERRIDE_BY_INLINE!>fun h(<!NOT_YET_SUPPORTED_IN_INLINE!>x: Int<!>, block: () -> String)<!> = block()
}

fun box() = A().h { "OK" }

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, inline, integerLiteral,
interfaceDeclaration, lambdaLiteral, override, stringLiteral */
