// FIR_COMPARISON
data class TestData(val first: Int, val second: String)

fun f() : TestData = TestData(42, "Second")

fun test() {
  val (dataFirst, dataSecond) = f()
  d<caret>
}

// EXIST: dataFirst, dataSecond