// RUN_PIPELINE_TILL: FRONTEND
data class <!CONFLICTING_OVERLOADS, CONFLICTING_OVERLOADS!>A(val x: Int, val y: String)<!> {
    <!CONFLICTING_OVERLOADS!>fun component1()<!> = 1
    <!CONFLICTING_OVERLOADS!>fun component2()<!> = 2
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, integerLiteral, primaryConstructor,
propertyDeclaration */
