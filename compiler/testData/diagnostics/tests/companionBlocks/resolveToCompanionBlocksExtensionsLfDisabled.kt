// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CompanionBlocks -CompanionExtensions
open class C {
    companion {
        fun foo() {}
        val bar = 1
        operator fun invoke(s: String) {}
    }

    fun test() {
        foo()
        bar
    }
}

class D : C() {
    fun testD() {
        foo()
        bar
    }
}

fun test() {
    C.foo()
    C.bar
    C("")
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, operator, propertyDeclaration,
stringLiteral */
