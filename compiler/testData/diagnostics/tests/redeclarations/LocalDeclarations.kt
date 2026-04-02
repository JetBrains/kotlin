// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +NestedTypeAliases +LocalTypeAliases

fun conflicts() {
    class <!CONFLICTING_OVERLOADS, REDECLARATION!>C<!>
    class <!CONFLICTING_OVERLOADS, REDECLARATION!>C<!>

    typealias <!REDECLARATION!>TA<!> = String
    typealias <!REDECLARATION!>TA<!> = String

    class <!REDECLARATION!>ClassConflictsTA<!>
    typealias <!REDECLARATION!>ClassConflictsTA<!> = Int
}

fun noConflics(condition: Boolean) {
    typealias TA = Char

    if (condition) {
        typealias TA = String
        val s: TA = "string"
    } else {
        typealias TA = Int
        val i: TA = 42
    }

    val c: TA = 'c'
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, integerLiteral, localClass, localProperty,
propertyDeclaration, stringLiteral, typeAliasDeclaration */
