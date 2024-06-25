// LANGUAGE: +VariableDeclarationInWhenSubject

fun test1(x: Any?) =
        when (val y = x) {
            is String -> "String, length = ${<!DEBUG_INFO_SMARTCAST!>y<!>.length}"
            null -> "Null"
            else -> "Any, hashCode = ${<!DEBUG_INFO_SMARTCAST!>y<!>.hashCode()}"
        }

fun test2() {
    when (val a: String? = "") {
        "test" -> <!DEBUG_INFO_SMARTCAST!>a<!>.length
        null -> {}
        else -> <!DEBUG_INFO_SMARTCAST!>a<!>.length
    }
}

fun foo(): String? = ""

fun test3() {
    when (val a = foo()) {
        null -> {}
        else -> <!DEBUG_INFO_SMARTCAST!>a<!>.length
    }
}

fun test4(s: String?) {
    when (val a = true) {
        <!CONFUSING_BRANCH_CONDITION_ERROR!>s != null<!> -> <!DEBUG_INFO_SMARTCAST!>s<!>.length
        else -> {}
    }
}
