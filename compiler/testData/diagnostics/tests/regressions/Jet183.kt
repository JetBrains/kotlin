enum class ProtocolState {
  WAITING {
    override fun signal() = ProtocolState.TALKING
  },

  TALKING {
    override fun signal() = ProtocolState.WAITING
  };

  abstract fun signal() : ProtocolState
}

fun box() {
   val <!UNUSED_VARIABLE!>x<!>: ProtocolState = ProtocolState.WAITING
}
