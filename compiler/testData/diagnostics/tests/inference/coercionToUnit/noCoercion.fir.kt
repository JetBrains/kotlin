// !WITH_NEW_INFERENCE
// !CHECK_TYPE

fun noCoercionLastExpressionUsedAsReturnArgument() {
    val a = {
        42
    }

    a checkType { <!UNRESOLVED_REFERENCE!>_<!><() -> Int>() }
}

fun noCoercionBlockHasExplicitType() {
    val b: () -> Int = {
        if (true) 42
    }
}

fun noCoercionBlockHasExplicitReturn() {
    val c = l@{
        if (true) return@l 42

        if (true) 239
    }
}

fun noCoercionInExpressionBody(): Unit = "hello"