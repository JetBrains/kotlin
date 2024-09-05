fun wrongAnd(x: Any, boolExpr: Boolean) {
    when (x) {
        is String <!SYNTAX!>&& boolExpr<!> -> "hello"
    }
}

fun comma(x: Any, boolExpr: Boolean) {
    when (x) {
        is String, is Int <!SYNTAX!>&& boolExpr<!> -> "hello"
    }
}

fun elseAndAnd(x: Boolean, boolExpr: Boolean) {
    when (x) {
        else <!SYNTAX!>&& boolExpr<!> -> "hello"
    }
}