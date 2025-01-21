// RUN_PIPELINE_TILL: FRONTEND

sealed interface I
class A : I
class B : I

data class Proxy<T : I>(val t: T)

fun StatementEmpty() {
    val x: Proxy<*> = Proxy(A())
    when (x.t) {
    }
}

fun StatementA() {
    val x: Proxy<*> = Proxy(A())
    when (x.t) {
        is A -> ""
    }
}

fun StatementB() {
    val x: Proxy<*> = Proxy(A())
    when (x.t) {
        is B -> ""
    }
}

fun StatementAB() {
    val x: Proxy<*> = Proxy(A())
    when (x.t) {
        is A -> ""
        is B -> ""
    }
}

fun StatementElse() {
    val x: Proxy<*> = Proxy(A())
    when (x.t) {
        else -> ""
    }
}

fun StatementAElse() {
    val x: Proxy<*> = Proxy(A())
    when (x.t) {
        is A -> ""
        else -> ""
    }
}

fun StatementBElse() {
    val x: Proxy<*> = Proxy(A())
    when (x.t) {
        is B -> ""
        else -> ""
    }
}


fun ExpressionEmpty() {
    val x: Proxy<*> = Proxy(A())
    val str = <!NO_ELSE_IN_WHEN!>when<!> (x.t) {
    }
}

fun ExpressionA() {
    val x: Proxy<*> = Proxy(A())
    val str = <!NO_ELSE_IN_WHEN!>when<!> (x.t) {
        is A -> ""
    }
}

fun ExpressionB() {
    val x: Proxy<*> = Proxy(A())
    val str = <!NO_ELSE_IN_WHEN!>when<!> (x.t) {
        is B -> ""
    }
}

fun ExpressionAB() {
    val x: Proxy<*> = Proxy(A())
    val str = when (x.t) {
        is A -> ""
        is B -> ""
    }
}

fun ExpressionElse() {
    val x: Proxy<*> = Proxy(A())
    val str = when (x.t) {
        else -> ""
    }
}

fun ExpressionAElse() {
    val x: Proxy<*> = Proxy(A())
    val str = when (x.t) {
        is A -> ""
        else -> ""
    }
}

fun ExpressionBElse() {
    val x: Proxy<*> = Proxy(A())
    val str = when (x.t) {
        is B -> ""
        else -> ""
    }
}
