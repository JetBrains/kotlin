// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-81687

annotation class Anno

fun c(arg: Any) { }

fun testAnnotated() {
    val a = <!EXPRESSION_EXPECTED!>@Anno class A<!>
    val b = <!EXPRESSION_EXPECTED, UNSUPPORTED_FEATURE!><!WRONG_ANNOTATION_TARGET!>@Anno<!> typealias B = Anno<!>
    c(<!EXPRESSION_EXPECTED!>@Anno <!LOCAL_OBJECT_NOT_ALLOWED!>object C<!><!>)
    val d = <!EXPRESSION_EXPECTED!>@Anno val D: Int<!>
    fun e() = <!EXPRESSION_EXPECTED!>@Anno var E = 5<!>
}

fun testRHS() {
    val a = 0 < <!EXPRESSION_EXPECTED!>class A<!>
    val b = 0 <!NONE_APPLICABLE!>+<!> object <!SYNTAX!>C<!> { }
    c(0 + <!EXPRESSION_EXPECTED, UNSUPPORTED_FEATURE!>typealias C = Anno<!>)
    val d = <!CONDITION_TYPE_MISMATCH!>0<!> && <!EXPRESSION_EXPECTED!>val D: Int = 5<!>
    val e = c(0) <!UNRESOLVED_REFERENCE!>+<!> <!EXPRESSION_EXPECTED!>var E: Any<!>
}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, annotationDeclaration, anonymousObjectExpression,
classDeclaration, comparisonExpression, functionDeclaration, integerLiteral, localClass, localFunction, localProperty,
objectDeclaration, propertyDeclaration, typeAliasDeclaration */
