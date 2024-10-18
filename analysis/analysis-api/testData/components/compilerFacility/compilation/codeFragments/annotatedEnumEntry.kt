// ISSUE: KT-72257
// MODULE: context

// FILE: lib.kt
@Target(AnnotationTarget.PROPERTY)
annotation class Ann(val value: String)

enum class Some(val value: String) {
    @Ann("A") E("O");
}


@Target(AnnotationTarget.PROPERTY)
annotation class AnnSome(val value: Some)
enum class Other(val value: String) {
    @AnnSome(Some.E) E("O");
}

// FILE: context.kt
fun test() {
    <caret_context>val x = 0
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
Other.E.value + "K"
