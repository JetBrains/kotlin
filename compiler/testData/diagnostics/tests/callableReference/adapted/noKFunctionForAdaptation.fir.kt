// SKIP_TXT
fun foo(x: String = "O"): String = x
fun bar(x: String = "K"): String = x

fun dump(dumpStrategy: String) {
    val k0: kotlin.reflect.KFunction0<String> = returnAdapter(<!ADAPTED_CALLABLE_REFERENCE_AGAINST_REFLECTION_TYPE!>::foo<!>) // Error: ADAPTED_CALLABLE_REFERENCE_AGAINST_REFLECTION_TYPE
    val k1: kotlin.reflect.KFunction0<String> = <!INITIALIZER_TYPE_MISMATCH!>::foo<!>
    // Should be error here, too
    val k2: kotlin.reflect.KFunction0<String> = <!INITIALIZER_TYPE_MISMATCH!>if (dumpStrategy == "KotlinLike") ::foo else ::bar<!>

    val f0: Function0<String> = returnAdapter(<!ADAPTED_CALLABLE_REFERENCE_AGAINST_REFLECTION_TYPE!>::foo<!>)
    val f1: Function0<String> = ::foo
    val f2: Function0<String> = if (dumpStrategy == "KotlinLike") ::foo else ::bar
}

fun returnAdapter(a: kotlin.reflect.KFunction0<String>) = a
