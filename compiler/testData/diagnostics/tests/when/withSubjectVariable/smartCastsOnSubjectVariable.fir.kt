// LANGUAGE: +VariableDeclarationInWhenSubject

fun test1(x: Any?) =
        when (val y = x) {
            is String -> "String, length = ${y.length}"
            null -> "Null"
            else -> "Any, hashCode = ${y.hashCode()}"
        }

fun test2() {
    when (val a: String? = "") {
        "test" -> a.length
        <!SENSELESS_NULL_IN_WHEN!>null<!> -> {}
        else -> a.length
    }
}

fun foo(): String? = ""

fun test3() {
    when (val a = foo()) {
        null -> {}
        else -> a.length
    }
}

fun test4(s: String?) {
    when (val a = true) {
        <!CONFUSING_BRANCH_CONDITION_ERROR!>s != null<!> -> s.length
        else -> {}
    }
}
