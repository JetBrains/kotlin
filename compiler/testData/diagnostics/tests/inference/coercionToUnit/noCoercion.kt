// !WITH_NEW_INFERENCE
// !CHECK_TYPE

fun noCoercionLastExpressionUsedAsReturnArgument() {
    val a = {
        42
    }

    a checkType { _<() -> Int>() }
}

fun noCoercionBlockHasExplicitType() {
    val <!UNUSED_VARIABLE!>b<!>: () -> Int = {
        <!TYPE_MISMATCH!>if (true) <!UNUSED_EXPRESSION!>42<!><!>
    }
}

fun noCoercionBlockHasExplicitReturn() {
    val <!UNUSED_VARIABLE!>c<!> = l@{
        if (true) return@l 42

        <!INVALID_IF_AS_EXPRESSION!>if<!> (true) 239
    }
}

fun noCoercionInExpressionBody(): Unit = <!TYPE_MISMATCH!>"hello"<!>