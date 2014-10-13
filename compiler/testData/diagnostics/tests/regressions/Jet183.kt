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
  <!NO_GENERICS_IN_SUPERTYPE_SPECIFIER!>X<!>

}



fun box() {
   val <!UNUSED_VARIABLE!>x<!>: ProtocolState = ProtocolState.WAITING
}
