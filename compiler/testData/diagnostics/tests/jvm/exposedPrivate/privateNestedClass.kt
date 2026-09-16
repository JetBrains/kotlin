// RUN_PIPELINE_TILL: CODEGEN
// DIAGNOSTICS: -NOTHING_TO_INLINE

class C {
    private class Nested {
        fun foo() = "OK"
    }

    private inline fun privateFun() = Nested().foo()
    internal inline fun test() = <!PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!>privateFun()<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inline, nestedClass, stringLiteral */
