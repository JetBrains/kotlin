// RUN_PIPELINE_TILL: CODEGEN
class A {
    <!DUPLICATE_CLASS_NAMES!>class B<!>
    init {
        <!DUPLICATE_CLASS_NAMES!>class B<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, init, localClass, nestedClass */
