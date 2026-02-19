// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-81191

open class C(val x: Int)

class C2 : C(
    x = run {
        var <!VARIABLE_WITH_NO_TYPE_NO_INITIALIZER!>m<!><!SYNTAX!><!> <!RETURN_TYPE_MISMATCH!>{}<!>
    },
)

/* GENERATED_FIR_TAGS: classDeclaration, lambdaLiteral, localProperty, primaryConstructor, propertyDeclaration */
