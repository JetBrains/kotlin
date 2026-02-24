/*package TODO example.trivialExport*/
/*@file:WitPackage(TODO example:trivial-export@1.0.0)*/
@WitInterface(/*TODO*/"example:trivial-export/im-exported@1.0.0")
external interface ImExported {
  fun foo(): Int
}

@WitExport/*(TODO)*/
object ImExportedImpl : ImExported{
  override fun foo(): Int{
    return 42
  }
}



fun box():String {
    return "OK"
}
