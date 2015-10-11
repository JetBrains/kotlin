// JET-17 Do not infer property types by the initializer before the containing scope is ready

class WithC() {
  val a = 1
  val b = <error>$a</error> // error here, but must not be
}