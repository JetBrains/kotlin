// JET-17 Do not infer property types by the initializer before the containing scope is ready

class WithC() {
  val a = 1
  val b = <warning descr="[BACKING_FIELD_USAGE_DEPRECATED] Backing field usage is deprecated here, soon it will be possible only in property accessors">$a</warning> // error here, but must not be
}