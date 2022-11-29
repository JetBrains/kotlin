// SKIP_TXT
fun foo(x: String = "O"): String = x
fun bar(x: String = "K"): String = x

fun dump(dumpStrategy: String) {
    val k0: kotlin.reflect.KFunction0<String> = <!INAPPLICABLE_CANDIDATE!>returnAdapter<!>(::<!UNRESOLVED_REFERENCE!>foo<!>) // Error: ADAPTED_CALLABLE_REFERENCE_AGAINST_REFLECTION_TYPE
    val k1: kotlin.reflect.KFunction0<String> = ::<!UNRESOLVED_REFERENCE!>foo<!>
    // Should be error here, too
    val k2: kotlin.reflect.KFunction0<String> = if (dumpStrategy == "KotlinLike") ::<!UNRESOLVED_REFERENCE!>foo<!> else ::<!UNRESOLVED_REFERENCE!>bar<!>

    val f0: Function0<String> = <!INAPPLICABLE_CANDIDATE!>returnAdapter<!>(::<!UNRESOLVED_REFERENCE!>foo<!>)
    val f1: Function0<String> = ::foo
    val f2: Function0<String> = if (dumpStrategy == "KotlinLike") ::foo else ::bar
}

fun returnAdapter(a: kotlin.reflect.KFunction0<String>) = a
