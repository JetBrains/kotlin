// RUN_PIPELINE_TILL: FRONTEND
fun useDeclaredVariables() {
    for ((a, b)<!SYNTAX!><!>) {
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!>
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>b<!>
    }
}

fun checkersShouldRun() {
    for ((@A a, _)<!SYNTAX!><!>) {

    }
}

annotation class A

/* GENERATED_FIR_TAGS: annotationDeclaration, forLoop, functionDeclaration, localProperty, propertyDeclaration,
unnamedLocalVariable */
