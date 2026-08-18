// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-87881
// WITH_STDLIB

class A

fun foo(bar: A) =
  if (<!USELESS_IS_CHECK!>bar !is A<!>) {
    <!CANNOT_INFER_PARAMETER_TYPE!>buildList<!> {
      <!ARGUMENT_TYPE_MISMATCH!><!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>f<!>()<!> //no crash in K2 if bar.f
      when {
        <!ARGUMENT_TYPE_MISMATCH, SMARTCAST_TO_TYPE_VARIABLE!>bar<!> == <!ARGUMENT_TYPE_MISMATCH!>when {
          true -> <!ARGUMENT_TYPE_MISMATCH!>bar<!>
          else -> <!ARGUMENT_TYPE_MISMATCH!>bar<!>
        }<!> -> <!ARGUMENT_TYPE_MISMATCH!>1<!>

        else -> <!ARGUMENT_TYPE_MISMATCH!>bar<!>
      }
    }
  }
  else {0}

fun A.f() {}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, funWithExtensionReceiver, functionDeclaration, ifExpression,
integerLiteral, isExpression, lambdaLiteral, smartcast, whenExpression */
