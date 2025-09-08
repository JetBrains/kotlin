// LANGUAGE: +ContextParameters

// MODULE: context

// FILE: contextParameter.kt
class Ctx1 {
    fun foo() = 10
}

class Ctx2 {
    fun boo(x: Int) = 10 + x
}


context(ctx2: Ctx2)
fun bar(x: Int) = ctx2.boo(x)

context(ctx1: Ctx1, ctx2: Ctx2)
fun check(x: Int) {
    <caret_context>ctx1.foo()
}

fun main() {
    context(Ctx1(), Ctx2()) {
        check(10)
    }
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
bar(x)