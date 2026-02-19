// DUMP_CODE

// MODULE: context
// FILE: context.kt

class A<T>

inline fun <reified T> foo1() {
    <caret_context>val x = 1
}

inline fun <reified R1, reified R2> foo2() {
    <caret_stack_0>foo1<Map<in Set<out R1>, A<in R2>>>()
}

fun main() {
    <caret_stack_1>foo2<String, Int>()
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
// CODE_FRAGMENT_IMPORT: kotlin.reflect.typeOf
typeOf<T>().toString()