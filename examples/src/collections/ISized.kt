package jet.collections.sized

trait ISized {
  val size : Int

  val isEmpty : Boolean
    get() = size == 0

  val isNonEmpty : Boolean
    get() = size != 0
}