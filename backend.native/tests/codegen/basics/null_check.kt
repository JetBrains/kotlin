//--- Test "eqeq" -------------------------------------------------------------//

fun check_eqeq(a: Any?) = a == null

fun null_check_eqeq1() : Boolean {
  return check_eqeq(Any())
}

fun null_check_eqeq2() : Boolean {
  return check_eqeq(null)
}

//--- Test "eqeqeq" -----------------------------------------------------------//

fun check_eqeqeq(a: Any?) = a === null

fun null_check_eqeqeq1() : Boolean {
  return check_eqeqeq(Any())
}

fun null_check_eqeqeq2() : Boolean {
  return check_eqeqeq(null)
}

fun main(args: Array<String>) {
  if (null_check_eqeq1())    throw Error()
  if (!null_check_eqeq2())   throw Error()
  if (null_check_eqeqeq1())  throw Error()
  if (!null_check_eqeqeq2()) throw Error()
}