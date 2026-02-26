@WitInterface("wasi:cli/run@0.2.9")
external interface Run {
  /**
  Run the program.
  */
  fun run(): /* Result<Unit> */ Int // cheating a bit, normally this returns a Result<Unit>, but as we don't have abi translation for that yet, I manually changed the signature to the correctly lowered one: i32
}

@WitInterface("example:trivial-import/im-imported@1.0.0")
external interface ImImported {
  @WitImport
  companion object Import : ImImported
  fun foo(): Int
}



@WitExport/*(TODO)*/
object RunImpl : Run{
  override fun run(): Int{
    return ImImported.foo() % 2
    // -> run should return 1
  }
}

fun box() = "OK"