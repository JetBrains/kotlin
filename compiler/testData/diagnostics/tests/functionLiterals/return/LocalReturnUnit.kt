// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE

fun test(a: Int) {
    val x = run f@{
      if (a > 0) return@f
      else return@f Unit
    }
    checkSubtype<Unit>(x)
}

/* GENERATED_FIR_TAGS: classDeclaration, comparisonExpression, funWithExtensionReceiver, functionDeclaration,
functionalType, ifExpression, infix, integerLiteral, lambdaLiteral, localProperty, nullableType, propertyDeclaration,
typeParameter, typeWithExtension */
