// LANGUAGE: +WhenGuards
// WITH_STDLIB
// DIAGNOSTICS: -SENSELESS_COMPARISON, -USELESS_IS_CHECK, -USELESS_CAST, -DUPLICATE_LABEL_IN_WHEN

typealias BooleanAlias = Boolean

fun earlyJumpInGuard(x: Any) {
    when (x) {
        is String if x == "10" <!USELESS_ELVIS!>?: throw Exception()<!> -> 3
        is String if true && throw Exception() -> 3
        is String if false || throw Exception() -> 3
        is String if {throw Exception()}() -> 3
        is String if true && x == "10" <!USELESS_ELVIS!>?: return Unit<!> -> 3
        is String if return Unit -> 3
    }

    for (i in 1 .. 10) {
        when (x) {
            is String if x == "" || break -> 100
            is BooleanAlias if x && continue -> 200
        }
    }
}