// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

fun foo(s : String?, b : Boolean) {
    if (s == null) return

    val s1 = if (b) "" else s
    s1 checkType { _<String>() }

    val s2 = s
    s2 checkType { _<String>() }
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, funWithExtensionReceiver, functionDeclaration,
functionalType, ifExpression, infix, lambdaLiteral, localProperty, nullableType, propertyDeclaration, smartcast,
stringLiteral, typeParameter, typeWithExtension */
