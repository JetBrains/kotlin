// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT

interface SomeInterface {
    val interfaceMethod: (() -> Int)?
}

fun someFun(someInterface: SomeInterface) {
    someInterface.<!UNSAFE_IMPLICIT_INVOKE_CALL!>interfaceMethod<!>()

    if (someInterface.interfaceMethod != null) {
        someInterface.<!UNSAFE_IMPLICIT_INVOKE_CALL!>interfaceMethod<!>()
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, functionalType, ifExpression, interfaceDeclaration,
nullableType, propertyDeclaration, smartcast */
