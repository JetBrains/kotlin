// RUN_PIPELINE_TILL: FRONTEND
fun main() {
    val x = <!UNRESOLVED_REFERENCE!>++<!>++<!SYNTAX!><!>
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, incrementDecrementExpression, localProperty, propertyDeclaration */
