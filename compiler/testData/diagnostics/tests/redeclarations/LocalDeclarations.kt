// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +NestedTypeAliases +LocalTypeAliases

fun conflicts() {
    class <!REDECLARATION!>C<!>
    class <!REDECLARATION!>C<!>

    <!TOPLEVEL_TYPEALIASES_ONLY!>typealias <!REDECLARATION!>TA<!> = String<!>
    <!TOPLEVEL_TYPEALIASES_ONLY!>typealias <!REDECLARATION!>TA<!> = String<!>

    class <!REDECLARATION!>ClassConflictsTA<!>
    <!TOPLEVEL_TYPEALIASES_ONLY!>typealias <!REDECLARATION!>ClassConflictsTA<!> = Int<!>
}

fun noConflics(condition: Boolean) {
    <!TOPLEVEL_TYPEALIASES_ONLY!>typealias TA = Char<!>

    if (condition) {
        <!TOPLEVEL_TYPEALIASES_ONLY!>typealias TA = String<!>
        val s: TA = "string"
    } else {
        <!TOPLEVEL_TYPEALIASES_ONLY!>typealias TA = Int<!>
        val i: TA = 42
    }

    val c: TA = 'c'
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, integerLiteral, localClass, localProperty,
propertyDeclaration, stringLiteral, typeAliasDeclaration */
