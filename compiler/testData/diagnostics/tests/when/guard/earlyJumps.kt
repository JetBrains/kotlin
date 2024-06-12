// LANGUAGE: +WhenGuards
// WITH_STDLIB
// DIAGNOSTICS: -SENSELESS_COMPARISON, -USELESS_IS_CHECK, -USELESS_CAST, -DUPLICATE_LABEL_IN_WHEN

typealias BooleanAlias = Boolean

fun earlyJumpInGuard(x: Any) {
    when (x) {
        is String <!UNSUPPORTED_FEATURE!>if x == "10" ?: throw Exception()<!> -> 3
        is String <!UNSUPPORTED_FEATURE!>if true && throw Exception()<!> -> 3
        is String <!UNSUPPORTED_FEATURE!>if false || throw Exception()<!> -> 3
        is String <!UNSUPPORTED_FEATURE!>if {throw Exception()}()<!> -> 3
        is String <!UNSUPPORTED_FEATURE!>if true && x == "10" ?: return Unit<!> -> 3
        is String <!UNSUPPORTED_FEATURE!>if return Unit<!> -> 3
    }

    for (i in 1 .. 10) {
        when (x) {
            is String <!UNSUPPORTED_FEATURE!>if x == "" || break<!> -> 100
            is BooleanAlias <!UNSUPPORTED_FEATURE!>if x && continue<!> -> 200
        }
    }
}