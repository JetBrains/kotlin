package streams.sequence.terminal

fun main(args: Array<String>) {
  //Breakpoint!
  sequenceOf(1, 2, 3).elementAtOrElse(3, { _ -> 3 })
}