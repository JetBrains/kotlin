// JET-17 Do not infer property types by the initializer before the containing scope is ready

class WithC() {
  val a = 1
  val b = <!BACKING_FIELD_USAGE_DEPRECATED!>$a<!> // error here, but must not be
}
