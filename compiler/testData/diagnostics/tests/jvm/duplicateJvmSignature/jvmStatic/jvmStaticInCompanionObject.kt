// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB

open class Base {
    fun foo() {}
}

class Derived : Base() {
    companion object {
        @JvmStatic <!ACCIDENTAL_OVERRIDE!>fun foo() {}<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, objectDeclaration */
