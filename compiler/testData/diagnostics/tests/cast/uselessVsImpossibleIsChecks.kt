// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76766
// RENDER_DIAGNOSTIC_ARGUMENTS

class A
class B


fun uselessAlwaysTrue(a: A) = <!USELESS_IS_CHECK("true")!>a is A<!>

fun uselessAlwaysFalse(a: A) = <!USELESS_IS_CHECK("false")!>a !is A<!>

fun impossibleAlwaysFalse(a: A) = <!IMPOSSIBLE_IS_CHECK_ERROR("false")!>a is B<!>

fun impossibleAlwaysTrue(a: A) = <!IMPOSSIBLE_IS_CHECK_ERROR("true")!>a !is B<!>


fun nullableUselessAlwaysTrue(a: A?) = <!USELESS_IS_CHECK("true")!>a is A?<!>

fun nullableUselessAlwaysFalse(a: A?) = <!USELESS_IS_CHECK("false")!>a !is A?<!>

fun nullableImpossibleAlwaysFalse(a: A?) = <!IMPOSSIBLE_IS_CHECK_RELYING_ON_NULL_ERROR("true")!>a is B?<!>

fun nullableImpossibleAlwaysTrue(a: A?) = <!IMPOSSIBLE_IS_CHECK_RELYING_ON_NULL_ERROR("false")!>a !is B?<!>


fun uselessAs(a: A) = a <!USELESS_CAST!>as A<!>

fun uselessNullaleAs(a: A) = a <!USELESS_CAST!>as? A<!>

fun impossibleAs(a: A) = a <!CAST_NEVER_SUCCEEDS!>as<!> B

fun impossibleNullableAs(a: A) = a <!CAST_NEVER_SUCCEEDS!>as?<!> B


fun nullableUselessAs(a: A?) = a <!USELESS_CAST!>as A?<!>

fun nullableUselessNullaleAs(a: A?) = a <!USELESS_CAST!>as? A?<!>

fun nullableImpossibleAs(a: A?) = a <!UNSAFE_CAST_RELYING_ON_NULL!>as<!> B?

fun nullableImpossibleNullableAs(a: A?) = a <!SAFE_CAST_RELYING_ON_NULL!>as?<!> B?


/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, isExpression */
