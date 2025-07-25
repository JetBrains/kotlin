// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +UnitConversionsOnArbitraryExpressions
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

fun foo(f: (Int, String) -> Unit) {}

abstract class SubInt : (Int, String) -> Int
abstract class SubIntWrong : (String, String) -> Int

fun test1(s: SubInt, sWrong: SubIntWrong) {
    foo(<!ARGUMENT_TYPE_MISMATCH!>s<!>)
    foo(<!ARGUMENT_TYPE_MISMATCH!>sWrong<!>)

    val a = "foo"
    foo(<!ARGUMENT_TYPE_MISMATCH!>a<!>)

    a <!CAST_NEVER_SUCCEEDS!>as<!> (Int, String) -> String
    foo(<!ARGUMENT_TYPE_MISMATCH!>a<!>)
}

fun <T> test2(x: T) where T : (Int, String) -> Int, T : (Double) -> Int {
    foo(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, functionalType, intersectionType,
localProperty, propertyDeclaration, smartcast, stringLiteral, typeConstraint, typeParameter */
