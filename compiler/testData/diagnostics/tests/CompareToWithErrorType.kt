// RUN_PIPELINE_TILL: FRONTEND
fun test() {
  if (<!UNRESOLVED_REFERENCE!>x<!> <!DEBUG_INFO_MISSING_UNRESOLVED!>><!> 0) {

  }
}

/* GENERATED_FIR_TAGS: comparisonExpression, functionDeclaration, ifExpression, integerLiteral */
