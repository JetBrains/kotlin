// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
class A

fun main() {
    val x = :: <!SYNTAX!><!>;
    val y = A::
<!SYNTAX!><!>}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, propertyDeclaration */
