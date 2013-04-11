data class LocalData(val first : Int, val second : Int)

val (localFirst, localSecond) = LocalData(11, 12)

fun test() {
  local<caret>
}

// Test that this test won't fail with exception
// Regression for EA-39175

// ABSENT: localFirst, localSecond
