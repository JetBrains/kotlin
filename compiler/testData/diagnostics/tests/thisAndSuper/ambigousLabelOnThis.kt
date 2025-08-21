// RUN_PIPELINE_TILL: BACKEND
class Dup {
  fun Dup() : Unit {
    this<!AMBIGUOUS_LABEL!>@Dup<!>
  }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, thisExpression */
