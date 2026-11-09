// LANGUAGE: +CompanionBlocks +CompanionExtensions

// MODULE: context

// FILE: context.kt
class Owner {
    companion {
        val prop: Int = 1
    }
}

companion fun Owner.test(p: Int) {
    <caret_context>val y = 0
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
p + prop
