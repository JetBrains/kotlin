// MODULE: context
// MODULE_KIND: ScriptSource
// GRADLE_LIKE_SCRIPT

// FILE: context.test.kts
<caret_context>val x = 0

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
projectApi()
