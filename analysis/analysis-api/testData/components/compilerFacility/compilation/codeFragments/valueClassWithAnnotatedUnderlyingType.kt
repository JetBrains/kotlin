// ISSUE: KT-87269
// DUMP_IR

// MODULE: common
// MODULE_KIND: Source
// FILE: source.kt
annotation class Ann(val type: KClass<*>)

@JvmInline
value class ValueClass(val value: @Ann(with = String::class) Int)

fun main() {
    val vc = ValueClass(1)
    <caret_context>println(vc) // ← breakpoint, evaluate `vc`
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: common

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
<caret>vc
