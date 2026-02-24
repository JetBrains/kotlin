@WitInterface("wasi:cli/run@0.2.9")
external interface Run {
  /**
  Run the program.
  */
  fun run(): Int
}

var x = 1

@WitExport/*(TODO)*/
object RunImpl : Run{
  override fun run(): Int{
    // no observable side effects yet sadge
    x = 42

    return 0
  }
}

fun box() = "OK"