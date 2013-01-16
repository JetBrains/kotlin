fun Int.foo() : Boolean = true

fun foo() : Int {
    val s = ""
    val x = 1
    when (x) {
      is <error>String</error> -> 1
      !is Int -> 1
      is Any<warning>?</warning> -> 1
      <error>s</error> -> 1
      1 -> 1
      1 + <error>a</error> -> 1
      in 1..<error>a</error> -> 1
      !in 1..<error>a</error> -> 1
      else -> 1
    }

    return 0
}

val _type_test : Int = foo() // this is needed to ensure the inferred return type of foo()

fun test() {
  val x = 1;
  val s = "";

  when (x) {
    <error>s</error> -> 1
    <error>""</error> -> 1
    x -> 1
    1 -> 1
    else -> 1
  }

  val z = 1

  when (z) {
    <error>else</error> -> 1
    <error>1 -> 2</error>
  }

  when (z) {
    else -> 1
  }
}