// COMPILATION_ERRORS

fun wrongAnd(x: Any, boolExpr: Boolean) {
    when (x) {
        is String && boolExpr -> "hello"
    }
}

fun comma(x: Any, boolExpr: Boolean) {
    when (x) {
        is String, is Int && boolExpr -> "hello"
    }
}

fun elseAndAnd(x: Boolean, boolExpr: Boolean) {
    when (x) {
        else && boolExpr -> "hello"
    }
}