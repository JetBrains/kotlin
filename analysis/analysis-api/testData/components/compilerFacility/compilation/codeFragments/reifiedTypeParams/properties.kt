// DUMP_CODE

// MODULE: context
// FILE: context.kt

inline var <reified T, reified R> Map<T, R>.inlineVar1: String
    get() {
        return ""
    }
    set(value) {
        <caret_context>val x = 1
    }

inline var <reified T, reified R> Map<T, R>.inlineVar2: String
    get() {
        return ""
    }
    set(value) {
        <caret_stack_0>inlineVar1 = ""
    }

inline val <reified T> T.inlineVal1: String
    get() {
        mapOf<T, Int>().<caret_stack_1>inlineVar2 = ""
        return ""
    }

inline val <reified T> T.inlineVal2: String
    get() {
        return <caret_stack_2>inlineVal1
    }

inline fun <reified T> foo1(x: T): String {
    return x.<caret_stack_3>inlineVal2
}

fun main() {
    <caret_stack_4>foo1("")
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
T::class.simpleName + "_" + R::class.simpleName