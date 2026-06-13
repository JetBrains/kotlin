// MODULE: context

// FILE: context.kt
package example

class StringBuffer

fun test() {
    <caret_context>StringBuffer()
}


// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: TYPE
<caret>StringBuffer
