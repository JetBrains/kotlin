// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE

fun foo() {
    val s: String? = if (true) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>materialize()<!> else null
}

fun <K> materialize(): K = TODO()
