// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// ISSUE: KT-82878

// FILE: a.kt
package a

fun bar() {}

fun String.gau() {}

// FILE: b.kt
package b

context(s: String)
fun bar() {}

context(s: String)
fun gau() {}

// FILE: test.kt
import a.*
import b.*

fun foo() {}

fun baz() {}

context(s: String)
<!CONTEXTUAL_OVERLOAD_SHADOWED("fun baz(): Unit")!>fun baz()<!> {}

fun test() {
    context(s: String)
    fun foo() {
        ::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>foo<!>
    }

    ::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>foo<!>

    val ctx = ""
    with(ctx) { ::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>foo<!> }
    context(ctx) { ::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>foo<!> }

    ::<!OVERLOAD_RESOLUTION_AMBIGUITY!>bar<!>

    ::<!OVERLOAD_RESOLUTION_AMBIGUITY!>baz<!>

    ::<!CALLABLE_REFERENCE_TO_CONTEXTUAL_DECLARATION!>gau<!>
}

fun String.test() {
    val ctx = ""
    String::gau
    ctx::gau
    ::gau

    String::<!UNRESOLVED_REFERENCE!>bar<!>
    ctx::<!UNRESOLVED_REFERENCE!>bar<!>
    ::<!OVERLOAD_RESOLUTION_AMBIGUITY!>bar<!>
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, functionDeclarationWithContext, localFunction */
