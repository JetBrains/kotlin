// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-81687

annotation class Anno

fun c(arg: Any) { }

fun testAnnotated() {
    val a = <!DECLARATION_IN_ILLEGAL_CONTEXT!>@Anno class A<!>
    val b = <!DECLARATION_IN_ILLEGAL_CONTEXT!>@<!DEBUG_INFO_MISSING_UNRESOLVED!>Anno<!> typealias B = <!DEBUG_INFO_MISSING_UNRESOLVED!>Anno<!><!>
    c(@<!DEBUG_INFO_MISSING_UNRESOLVED!>Anno<!> <!DECLARATION_IN_ILLEGAL_CONTEXT!>object C<!>)
    val d = <!DECLARATION_IN_ILLEGAL_CONTEXT!>@Anno val D: Int<!>
    fun e() = <!DECLARATION_IN_ILLEGAL_CONTEXT!>@Anno var E = 5<!>
}

fun testRHS() {
    val a = 0 < <!DECLARATION_IN_ILLEGAL_CONTEXT!>class A<!>
    val b = 0 <!NONE_APPLICABLE!>+<!> object <!SYNTAX!>C<!> { }
    c(0 + <!DECLARATION_IN_ILLEGAL_CONTEXT!>typealias C = <!DEBUG_INFO_MISSING_UNRESOLVED!>Anno<!><!>)
    val d = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>0<!> && <!DECLARATION_IN_ILLEGAL_CONTEXT, EXPECTED_TYPE_MISMATCH!>val D: Int = 5<!>
    val e = c(0) <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>+<!> <!DECLARATION_IN_ILLEGAL_CONTEXT!>var E: Any<!>
}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, annotationDeclaration, anonymousObjectExpression,
classDeclaration, comparisonExpression, functionDeclaration, integerLiteral, localClass, localFunction, localProperty,
objectDeclaration, propertyDeclaration, typeAliasDeclaration */
