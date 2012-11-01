class Dup {
  fun Dup() : Unit {
    this<!AMBIGUOUS_LABEL!>@Dup<!>
  }
}