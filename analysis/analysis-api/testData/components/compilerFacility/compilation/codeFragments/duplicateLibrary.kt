// ATTACH_DUPLICATE_STDLIB

// MODULE: context

// FILE: context.kt
fun test(text: String) {
    <caret_context>consume(text)
}

fun consume(text: String) {}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
text.takeUnless { it.isEmpty() }?.length