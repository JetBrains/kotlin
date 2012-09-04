val testExternal

class Some(testParam : Int) {
  private val myVal : Int

  {
    val testing = 12
    myVal = test<caret>
  }
}

// EXIST: testParam, testExternal, testing
