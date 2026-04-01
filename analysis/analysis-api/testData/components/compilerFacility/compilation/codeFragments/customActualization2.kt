// LANGUAGE: +MultiPlatformProjects
// ACTUALIZATION: common->jvm2

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt
fun test() {
    <caret_context>Unit
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: common
// COMPILATION_ERRORS
// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
foo()


// MODULE: jvm1()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm1.kt
fun foo() {}



// MODULE: jvm2()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm2.kt
fun bar() {}