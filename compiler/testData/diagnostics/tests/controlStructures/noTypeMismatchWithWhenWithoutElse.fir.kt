// ISSUE: KT-62069

enum class ExhaustiveEnum { ONE, TWO }

fun exhaust(ee: ExhaustiveEnum) {
    var v = 0
    v = <!NO_ELSE_IN_WHEN!>when<!> (ee) {
        ExhaustiveEnum.ONE -> 1
    }
    v = <!INVALID_IF_AS_EXPRESSION!>if<!> (ee == ExhaustiveEnum.ONE) 2
}
