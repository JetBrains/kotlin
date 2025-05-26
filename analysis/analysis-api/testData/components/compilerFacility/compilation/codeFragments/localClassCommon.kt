// MODULE: context
// TARGET_PLATFORM: Common

//FILE: context.kt
fun main() {
    class Foo
    <caret_context>output("hi")
}

fun output(text: String) {}


// MODULE: jvm()()(context)
// TARGET_PLATFORM: JVM
// We need a JVM module to get the JDK against which we will compile code

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
Foo()