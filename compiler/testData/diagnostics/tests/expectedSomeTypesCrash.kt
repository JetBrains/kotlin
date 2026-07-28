// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-87881
// WITH_STDLIB

class A

fun foo(bar: A) =
  if (bar !is A) {
    buildList {
      f() //no crash in K2 if bar.f
      when {
        bar == when {
          true -> bar
          else -> bar
        } -> 1

        else -> bar
      }
    }
  }
  else {0}

fun A.f() {}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, funWithExtensionReceiver, functionDeclaration, ifExpression,
integerLiteral, isExpression, lambdaLiteral, smartcast, whenExpression */
