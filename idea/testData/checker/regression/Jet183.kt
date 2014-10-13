enum class ProtocolState {
  WAITING {
    override fun signal() = ProtocolState.TALKING
  }

  TALKING {
    override fun signal() = ProtocolState.WAITING
  }

  abstract fun signal() : ProtocolState
}

enum class Foo<T> {
  <error>X</error>

}



fun box() {
   val <warning>x</warning>: ProtocolState = ProtocolState.WAITING
}
