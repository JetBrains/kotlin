// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76766
// RENDER_DIAGNOSTIC_ARGUMENTS

class A
class B


fun uselessAlwaysTrue(a: A) = <!USELESS_IS_CHECK!>a is A<!>

fun uselessAlwaysFalse(a: A) = <!USELESS_IS_CHECK!>a !is A<!>

fun impossibleAlwaysFalse(a: A) = a is <!INCOMPATIBLE_TYPES!>B<!>

fun impossibleAlwaysTrue(a: A) = a !is <!INCOMPATIBLE_TYPES!>B<!>


fun nullableUselessAlwaysTrue(a: A?) = <!USELESS_IS_CHECK!>a is A?<!>

fun nullableUselessAlwaysFalse(a: A?) = <!USELESS_IS_CHECK!>a !is A?<!>

fun nullableImpossibleAlwaysFalse(a: A?) = a is <!INCOMPATIBLE_TYPES!>B?<!>

fun nullableImpossibleAlwaysTrue(a: A?) = a !is <!INCOMPATIBLE_TYPES!>B?<!>


fun uselessAs(a: A) = a <!USELESS_CAST!>as A<!>

fun uselessNullaleAs(a: A) = a as? A

fun impossibleAs(a: A) = a <!CAST_NEVER_SUCCEEDS!>as<!> B

fun impossibleNullableAs(a: A) = a <!CAST_NEVER_SUCCEEDS!>as?<!> B


fun nullableUselessAs(a: A?) = a <!USELESS_CAST!>as A?<!>

fun nullableUselessNullaleAs(a: A?) = a <!USELESS_CAST!>as? A?<!>

fun nullableImpossibleAs(a: A?) = a as B?

fun nullableImpossibleNullableAs(a: A?) = a as? B?


/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, isExpression */
