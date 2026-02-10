// RUN_PIPELINE_TILL: FRONTEND
class A(val x: (String.() -> Unit)?)

fun test(a: A) {
    if (a.x != null) {
        "".(a.x)()
        a.x("") // todo
        (a.x)("")
    }
    "".<!UNSAFE_IMPLICIT_INVOKE_CALL!>(a.x)<!>()
    a.<!UNSAFE_IMPLICIT_INVOKE_CALL!>x<!>("")
    <!UNSAFE_IMPLICIT_INVOKE_CALL!>(a.x)<!>("")

    with("") {
        <!NO_VALUE_FOR_PARAMETER!>a.<!UNSAFE_IMPLICIT_INVOKE_CALL!>x<!>()<!>
        <!UNSAFE_IMPLICIT_INVOKE_CALL!>(a.x)<!>()
        if (a.x != null) {
            <!NO_VALUE_FOR_PARAMETER!>a.x()<!> // todo
            (a.x)()
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, functionalType, ifExpression,
lambdaLiteral, nullableType, primaryConstructor, propertyDeclaration, smartcast, stringLiteral, typeWithExtension */
