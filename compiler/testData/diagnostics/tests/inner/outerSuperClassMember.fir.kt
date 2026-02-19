// RUN_PIPELINE_TILL: FRONTEND
open class Base {
    fun foo() {}
}

class Derived : Base() {
    class Nested {
        fun bar() = <!INACCESSIBLE_OUTER_CLASS_RECEIVER!>foo<!>()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nestedClass */
