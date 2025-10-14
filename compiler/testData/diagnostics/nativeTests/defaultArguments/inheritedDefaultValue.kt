// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ForbidOverriddenDefaultParametersInInline
// ISSUE: KT-49722

interface I {
    fun h(x: Int = 3, block: () -> String): Any
}

open class A : I {
    // KT-49722: [NOT_YET_SUPPORTED_IN_INLINE] should be raised for the next line, similar to K/JVM test `inheritedDefaultValue.kt`
    final override inline <!OVERRIDE_BY_INLINE!>fun h(<!NOT_YET_SUPPORTED_IN_INLINE!>x: Int<!>, block: () -> String)<!> = block()
}

fun box() = A().h { "OK" }

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, inline, integerLiteral,
interfaceDeclaration, lambdaLiteral, override, stringLiteral */
