// RUN_PIPELINE_TILL: FRONTEND
fun main() {
    val <!UNUSED_VARIABLE!>y<!> = ++<!VARIABLE_EXPECTED!><!VARIABLE_EXPECTED!>5<!>++<!>
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, incrementDecrementExpression, localProperty, propertyDeclaration */
