val some = 12
class Some(someParam : Int) {
  {
    fun internalFun(someInternal : Int) : Int {
      return some<caret>
    }
  }
}

// EXIST: some, someInternal, someParam

