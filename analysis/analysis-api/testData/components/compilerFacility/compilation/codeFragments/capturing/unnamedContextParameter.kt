// LANGUAGE: +ContextParameters

// MODULE: context

// FILE: contextParameter.kt
class Ctx1 {
    fun foo() = 10
}

class Ctx2 {
    fun boo(x: Int) = 10 + x
}


context(ctx2: Ctx2, ctx1: Ctx1)
fun bar(x: Int) = ctx2.boo(x) + ctx1.foo()

context(_: Ctx1, _: Ctx2)
fun check(x: Int) {
    <caret_context>bar(x)
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