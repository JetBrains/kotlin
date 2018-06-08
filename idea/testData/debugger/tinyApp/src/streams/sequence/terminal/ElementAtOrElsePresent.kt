package streams.sequence.terminal

fun main(args: Array<String>) {
  //Breakpoint!
  sequenceOf(1, 2, 3).elementAtOrElse(2, { _ -> 3 })
}