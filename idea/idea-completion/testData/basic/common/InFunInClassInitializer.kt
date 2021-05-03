val some = 12
class Some(someParam : Int) {
  init {
    fun internalFun(someInternal : Int) : Int {
      return some<caret>
    }
  }
}

// EXIST: some, someInternal, someParam
// FIR_COMPARISON