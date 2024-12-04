// RUN_PIPELINE_TILL: BACKEND
class Dup {
  fun Dup() : Unit {
    this<!AMBIGUOUS_LABEL!>@Dup<!>
  }
}