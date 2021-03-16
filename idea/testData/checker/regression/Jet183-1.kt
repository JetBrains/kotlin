// FIR_IDENTICAL

enum class ProtocolState {
  WAITING {
    override fun signal() = ProtocolState.TALKING
  },

  TALKING {
    override fun signal() = ProtocolState.WAITING
  };

  abstract fun signal() : ProtocolState
}


fun box(): String {
   var x: ProtocolState = ProtocolState.WAITING
   x = x.signal()
   if (x != ProtocolState.TALKING) return "fail 1"
   x = x.signal()
   if (x != ProtocolState.WAITING) return "fail 2"
   return "OK"
}
