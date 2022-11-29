// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE

fun foo() {
    val s: String? = if (true) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>materialize()<!> else null
}

fun <K> materialize(): K = TODO()
