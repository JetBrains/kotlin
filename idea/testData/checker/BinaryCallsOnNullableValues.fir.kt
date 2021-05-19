class A() {
  override fun equals(a : Any?) : Boolean = false
}

fun f(): Unit {
  var x: Int? = 1
  x = 1
  x + 1
  x.plus(1)
  x < 1
  x += 1

  x == 1
  x != 1

  <error descr="[EQUALITY_NOT_APPLICABLE] Operator '==' cannot be applied to 'A' and 'kotlin/Int'">A() == 1</error>

  <error descr="[EQUALITY_NOT_APPLICABLE] Operator '===' cannot be applied to 'kotlin/Int' and 'kotlin/String'">x === "1"</error>
  <error descr="[EQUALITY_NOT_APPLICABLE] Operator '!==' cannot be applied to 'kotlin/Int' and 'kotlin/String'">x !== "1"</error>

  x === 1
  x !== 1

  x..2
  x in 1..2

  val y : Boolean? = true
  false || <error descr="[CONDITION_TYPE_MISMATCH] Condition type mismatch: inferred type is kotlin/Boolean? but Boolean was expected">y</error>
  <error descr="[CONDITION_TYPE_MISMATCH] Condition type mismatch: inferred type is kotlin/Boolean? but Boolean was expected">y</error> && true
  <error descr="[CONDITION_TYPE_MISMATCH] Condition type mismatch: inferred type is kotlin/Boolean? but Boolean was expected">y</error> && <error descr="[CONDITION_TYPE_MISMATCH] Condition type mismatch: inferred type is kotlin/Int but Boolean was expected">1</error>
}
