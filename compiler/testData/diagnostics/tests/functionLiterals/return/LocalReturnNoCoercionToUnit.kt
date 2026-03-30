// RUN_PIPELINE_TILL: FRONTEND
fun test(a: Int) {
    run f@{
      if (a > 0) return@f
      else return@f <!RETURN_TYPE_MISMATCH!>1<!>
    }
}

/* GENERATED_FIR_TAGS: comparisonExpression, functionDeclaration, ifExpression, integerLiteral, lambdaLiteral */
