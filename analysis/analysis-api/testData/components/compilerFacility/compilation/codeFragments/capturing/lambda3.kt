// MODULE: context
// ISSUE: KT-68694

// FILE: context.kt

fun main() {
    EnumClass.ALFA.callPrivateLam()
}

enum class EnumClass(private val lam: () -> String) {
    ALFA({ "ALFA" });

    fun callPrivateLam(): String {
        return <caret_context>lam()
    }
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
lam()

