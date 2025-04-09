// ISSUE: KT-71416
// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE

// IGNORE_BACKEND: JS_IR
// ^^^ KT-76547: Error with pre-serialization public inliner:
// Internal error in body lowering: java.lang.IllegalStateException:
// Cannot deserialize inline function from a non-Kotlin library: FUN IR_EXTERNAL_DECLARATION_STUB name:internalInlineFun visibility:internal modality:FINAL <> (<this>:<root>.A) returnType:kotlin.String [inline]
// Function source: null

// MODULE: lib
// FILE: A.kt
class A {
    private companion object {
        fun foo() = "OK"
    }

    private inline fun privateFun() = foo()
    internal inline fun internalInlineFun() = <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_CASCADING_ERROR!>privateFun()<!>
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return A().internalInlineFun()
}
