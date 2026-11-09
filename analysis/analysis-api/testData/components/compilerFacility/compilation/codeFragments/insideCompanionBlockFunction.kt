// LANGUAGE: +CompanionBlocks +CompanionExtensions

// MODULE: context

// FILE: context.kt
class Owner {
    val member: Int = 0

    companion {
        val prop: Int = 1

        fun test(p: Int) {
            <caret_context>val y = 0
        }

        private fun privateFoo(): Int = 2
    }
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
p + prop + privateFoo()
