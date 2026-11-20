// RUN_PIPELINE_TILL: FRONTEND
error class E1
error class E2

fun isChecks(a: Any, s: String, i: Int, u: String | E1) {
    val _ = a is (String | E1)
    val _ = a is <!CANNOT_CHECK_FOR_ERASED!>(List<String> | E1)<!>
    val _ = a is (List<*> | E1)
    val _ = <!USELESS_IS_CHECK!>s is (String | E1)<!>
    val _ = <!IMPOSSIBLE_IS_CHECK_ERROR!>i is (String | E1)<!>

    val _ = a !is (String | E1)
    val _ = a !is <!CANNOT_CHECK_FOR_ERASED!>(List<String> | E1)<!>
    val _ = a !is (List<*> | E1)
    val _ = <!USELESS_IS_CHECK!>s !is (String | E1)<!>
    val _ = <!IMPOSSIBLE_IS_CHECK_ERROR!>i !is (String | E1)<!>

    val _ = u !is String
    val _ = <!IMPOSSIBLE_IS_CHECK_ERROR!>u !is Int<!>

    val _ = u is String
    val _ = u is E1
    val _ = <!IMPOSSIBLE_IS_CHECK_ERROR!>u is Int<!>
    val _ = u is (String | E1)
    val _ = u is (Int | E1)
    val _ = <!IMPOSSIBLE_IS_CHECK_ERROR!>u is (Int | E2)<!>
}

fun cast(a: Any, s: String, i: Int, u: String | E1) {
    if (true) { val _ = a as (String | E1) }
    if (true) { val _ = a <!UNCHECKED_CAST!>as (List<String> | E1)<!> }
    if (true) { val _ = a as (List<*> | E1) }
    if (true) { val _ = s as (String | E1) }
    if (true) { val _ = i <!CAST_NEVER_SUCCEEDS!>as<!> (String | E1) }

    if (true) { val _ = u as String }
    if (true) { val _ = u as E1 }
    if (true) { val _ = u <!CAST_NEVER_SUCCEEDS!>as<!> Int }
    if (true) { val _ = u as (String | E1) }
    if (true) { val _ = u as (Int | E1) }
    if (true) { val _ = u <!CAST_NEVER_SUCCEEDS!>as<!> (Int | E2) }
}

fun safeCast(a: Any, s: String, i: Int, u: String | E1) {
    if (true) { val _ = a as? (String | E1) }
    if (true) { val _ = a <!UNCHECKED_CAST!>as? (List<String> | E1)<!> }
    if (true) { val _ = a as? (List<*> | E1) }
    if (true) { val _ = s as? (String | E1) }
    if (true) { val _ = i <!CAST_NEVER_SUCCEEDS!>as?<!> (String | E1) }

    if (true) { val _ = u as? String }
    if (true) { val _ = u as? E1 }
    if (true) { val _ = u <!CAST_NEVER_SUCCEEDS!>as?<!> Int }
    if (true) { val _ = u as? (String | E1) }
    if (true) { val _ = u as? (Int | E1) }
    if (true) { val _ = u <!CAST_NEVER_SUCCEEDS!>as?<!> (Int | E2) }
}

interface I<T>
interface J<T> : I<T>

fun generic(i: I<String>, u: I<String> | E1) {
    val _ = i is (J<String> | E1)
    val _ = i !is (J<String> | E1)

    val _ = u is J<String>
    val _ = u !is J<String>
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, ifExpression, isExpression, localProperty,
nullableType, propertyDeclaration, unnamedLocalVariable */
