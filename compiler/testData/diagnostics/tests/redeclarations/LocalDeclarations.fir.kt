// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +NestedTypeAliases

fun conflicts() {
    class <!CONFLICTING_OVERLOADS!>C<!>
    class <!CONFLICTING_OVERLOADS!>C<!>

    typealias TA = String
    typealias TA = String

    class ClassConflictsTA
    typealias ClassConflictsTA = Int
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
