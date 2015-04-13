package second

class Some {
  // Two function to prevent automatic insert
  fun testFunction1() : Int = 12
  fun testFunction2() : Int = 12
}

fun someWithLiteral(body: (Some) -> Unit): Int = 12