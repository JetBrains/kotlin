// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions

open class A() {
    fun fooA() = "OK"

    companion {
        fun barA() = "OK"
    }
}

open class B : A()

class C : B() {
    fun barC() = barA()

    companion {
        fun fooC() = <!UNRESOLVED_REFERENCE!>fooA<!>()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, primaryConstructor, stringLiteral */
