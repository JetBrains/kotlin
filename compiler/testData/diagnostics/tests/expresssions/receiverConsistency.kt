// RUN_PIPELINE_TILL: FRONTEND
fun foo() {}

class C {
    fun bar() {}
    fun err() {}

    class Nested {
        fun test() {
            <!INACCESSIBLE_OUTER_CLASS_RECEIVER!>err<!>()
        }
    }
}

fun test() {
    val c = C()
    foo()
    c.bar()

    val err = C()
    err.<!UNRESOLVED_REFERENCE!>foo<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, nestedClass, propertyDeclaration */
