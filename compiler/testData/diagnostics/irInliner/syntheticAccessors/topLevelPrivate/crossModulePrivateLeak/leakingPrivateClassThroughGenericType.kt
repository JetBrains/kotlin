// ISSUE: KT-71416
// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE

// IGNORE_BACKEND: JS_IR
// ^^^ KT-76547: Error with pre-serialization public inliner:
// Internal error in body lowering: java.lang.IllegalStateException:
// Cannot deserialize inline function from a non-Kotlin library: FUN IR_EXTERNAL_DECLARATION_STUB name:inlineFun visibility:internal modality:FINAL <> () returnType:kotlin.String [inline]
// Function source: null

// MODULE: lib
// FILE: a.kt
private class Private

private inline fun <reified T> parameterized(): String {
    if (T::class == Private::class) return "OK"
    return T::class.simpleName ?: "Unknown type"
}

internal inline fun inlineFun() = <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_CASCADING_ERROR!>parameterized<Private>()<!>

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return inlineFun()
}
