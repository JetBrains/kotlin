@WitInterface("wasi:cli/run@0.2.9")
external interface Run {
  /**
  Run the program.
  */
  fun run(): Result<Unit>
}

var x = 1

@WitExport/*(TODO)*/
object RunImpl : Run{
  override fun run(): Result<Unit>{
    // no observable side effects yet sadge
    x = 42

    return Result.success(Unit)
  }
}

fun box() = "OK"