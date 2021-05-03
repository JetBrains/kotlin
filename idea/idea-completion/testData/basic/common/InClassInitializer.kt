val testExternal

class Some(testParam : Int) {
  private val myVal : Int

  init {
    val testing = 12
    myVal = test<caret>
  }
}

// EXIST: testParam, testExternal, testing
// FIR_COMPARISON