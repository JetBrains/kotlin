// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -DUPLICATE_CLASS_NAMES
// KT-3525
object B {
    class <!REDECLARATION!>C<!>
    class <!REDECLARATION!>C<!>

    val <!REDECLARATION!>a<!> : Int = 1
    val <!REDECLARATION!>a<!> : Int = 1
}

/* GENERATED_FIR_TAGS: classDeclaration, integerLiteral, nestedClass, objectDeclaration, propertyDeclaration */
