// RUN_PIPELINE_TILL: CODEGEN
// FIR_DUMP
// WITH_STDLIB
// ISSUES: KT-81850

fun foo() {}

fun test1(args: List<String>) {
  for (arg in args) [{ foo() }]
}

fun test2(cond: () -> Boolean) {
  while (cond()) [{ foo() }]
}

fun test3(cond: () -> Boolean) {
  do [{ foo() }] while (cond())
}

/* GENERATED_FIR_TAGS: doWhileLoop, forLoop, functionDeclaration, functionalType, lambdaLiteral, localProperty,
propertyDeclaration, whileLoop */
