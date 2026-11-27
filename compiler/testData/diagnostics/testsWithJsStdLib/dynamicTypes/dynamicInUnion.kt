// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +RichErrors

fun foo(
    x: <!NON_ERROR_COMPONENT_WRONG_POSITION_IN_UNION_TYPE!>String | dynamic<!>,
    y: <!DYNAMIC_IN_UNION_TYPE!>dynamic | RichError<!>,
    z: <!DYNAMIC_IN_UNION_TYPE!>TA<dynamic><!>,
) { }

typealias TA<T> = List<T | RichError>
