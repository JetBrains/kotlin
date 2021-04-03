fun Int.foo() : Boolean = true

fun foo() : Int {
    val s = ""
    val x = 1
    when (x) {
      is String -> 1
      !is Int -> 1
      is Any? -> 1
      <error descr="[INCOMPATIBLE_TYPES] Incompatible types: kotlin/Int and kotlin/String">s</error> -> 1
      1 -> 1
      1 <error descr="[OVERLOAD_RESOLUTION_AMBIGUITY] Overload resolution ambiguity between candidates: [kotlin/Int.plus, kotlin/Int.plus, kotlin/Int.plus, ...]">+</error> <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: a">a</error> -> 1
      in 1..<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: a">a</error> -> 1
      !in 1..<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: a">a</error> -> 1
      else -> 1
    }

    return 0
}

val _type_test : Int = foo() // this is needed to ensure the inferred return type of foo()

fun test() {
  val x = 1;
  val s = "";

  when (x) {
    <error descr="[INCOMPATIBLE_TYPES] Incompatible types: kotlin/Int and kotlin/String">s</error> -> 1
    <error descr="[INCOMPATIBLE_TYPES] Incompatible types: kotlin/Int and kotlin/String">""</error> -> 1
    x -> 1
    1 -> 1
    else -> 1
  }

  val z = 1

  when (z) {
    <error descr="[ELSE_MISPLACED_IN_WHEN] ">else</error> -> 1
    1 -> 2
  }

  when (z) {
    else -> 1
  }
}
