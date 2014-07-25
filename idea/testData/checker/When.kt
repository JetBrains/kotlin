fun Int.foo() : Boolean = true

fun foo() : Int {
    val s = ""
    val x = 1
    when (x) {
      is <error>String</error> -> <warning>1</warning>
      !is Int -> <warning>1</warning>
      is Any<warning>?</warning> -> <warning>1</warning>
      <error>s</error> -> <warning>1</warning>
      1 -> <warning>1</warning>
      1 + <error>a</error> -> <warning>1</warning>
      in 1..<error>a</error> -> <warning>1</warning>
      !in 1..<error>a</error> -> <warning>1</warning>
      else -> <warning>1</warning>
    }

    return 0
}

val _type_test : Int = foo() // this is needed to ensure the inferred return type of foo()

fun test() {
  val x = 1;
  val s = "";

  when (x) {
    <error>s</error> -> <warning>1</warning>
    <error>""</error> -> <warning>1</warning>
    x -> <warning>1</warning>
    1 -> <warning>1</warning>
    else -> <warning>1</warning>
  }

  val z = 1

  when (z) {
    <error>else</error> -> <warning>1</warning>
    <warning>1 -> 2</warning>
  }

  when (<warning>z</warning>) {
    else -> <warning>1</warning>
  }
}