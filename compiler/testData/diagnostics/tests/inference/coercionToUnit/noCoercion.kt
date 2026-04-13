// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

fun noCoercionLastExpressionUsedAsReturnArgument() {
    val a = {
        42
    }

    a checkType { _<() -> Int>() }
}

fun noCoercionBlockHasExplicitType() {
    val b: () -> Int = {
        <!RETURN_TYPE_MISMATCH!>if (true) 42<!>
    }
}

fun noCoercionBlockHasExplicitReturn() {
    val c = l@{
        if (true) return@l 42

        if (true) 239
    }
}

fun noCoercionInExpressionBody(): Unit = <!RETURN_TYPE_MISMATCH!>"hello"<!>

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, ifExpression,
infix, integerLiteral, lambdaLiteral, localProperty, nullableType, propertyDeclaration, stringLiteral, typeParameter,
typeWithExtension */
