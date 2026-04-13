// RUN_PIPELINE_TILL: FRONTEND
fun test() {
  if (<!UNRESOLVED_REFERENCE!>x<!> > 0) {

  }
}

/* GENERATED_FIR_TAGS: comparisonExpression, functionDeclaration, ifExpression, integerLiteral */
