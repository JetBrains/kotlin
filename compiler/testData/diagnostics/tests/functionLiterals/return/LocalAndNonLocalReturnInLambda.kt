// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE

fun test2(a: Int) {
    val x = run f@{
      if (a > 0) <!RETURN_NOT_ALLOWED!>return<!>
      return@f 1
    }
    checkSubtype<Int>(x)
}

fun <T> run(f: () -> T): T { return f() }

/* GENERATED_FIR_TAGS: classDeclaration, comparisonExpression, funWithExtensionReceiver, functionDeclaration,
functionalType, ifExpression, infix, integerLiteral, lambdaLiteral, localProperty, nullableType, propertyDeclaration,
typeParameter, typeWithExtension */
