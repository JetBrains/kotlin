// RUN_PIPELINE_TILL: BACKEND
interface D {
  fun foo() {}
}

fun test(d: Any?) {
  if (d !is D) return

  class Local : D by d {
  }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, inheritanceDelegation, interfaceDeclaration,
isExpression, localClass, nullableType, smartcast */
