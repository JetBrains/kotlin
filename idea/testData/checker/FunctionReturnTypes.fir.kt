// COMPILER_ARGUMENTS: -XXLanguage:-NewInference
// RUNTIME

fun none() {}

fun unitEmptyInfer() {}
fun unitEmpty() : Unit {}
fun unitEmptyReturn() : Unit {return}
fun unitIntReturn() : Unit {return 1}
fun unitUnitReturn() : Unit {return Unit}
fun test1() : Any = { return }
fun test2() : Any = a@ {return@a 1}
fun test3() : Any { return }

fun bbb() {
  return 1
}

fun foo(expr: StringBuilder): Int {
  val c = 'a'
  when(c) {
    0.toChar() -> throw Exception("zero")
    else -> throw Exception("nonzero" + c)
  }
}


fun unitShort() : Unit = Unit
fun unitShortConv() : Unit = 1
fun unitShortNull() : Unit = null

fun intEmpty() : Int {}
fun intShortInfer() = 1
fun intShort() : Int = 1
//fun intBlockInfer()  {1}
fun intBlock() : Int {return 1}
fun intBlock1() : Int {1}

fun intString(): Int = "s"
fun intFunctionLiteral(): Int = { 10 }

fun blockReturnUnitMismatch() : Int {return}
fun blockReturnValueTypeMismatch() : Int {return 3.4}
fun blockReturnValueTypeMatch() : Int {return 1}
fun blockReturnValueTypeMismatchUnit() : Int {return Unit}

fun blockAndAndMismatch() : Int {
  true && false
}
fun blockAndAndMismatch1() : Int {
  return true && false
}
fun blockAndAndMismatch2() : Int {
  (return true) && (return false)
}

fun blockAndAndMismatch3() : Int {
  true || false
}
fun blockAndAndMismatch4() : Int {
  return true || false
}
fun blockAndAndMismatch5() : Int {
  (return true) || (return false)
}
fun blockReturnValueTypeMatch1() : Int {
  return if (1 > 2) 1.0 else 2.0
}
fun blockReturnValueTypeMatch2() : Int {
    return <error descr="[INVALID_IF_AS_EXPRESSION] 'if' must have both main and 'else' branches if used as an expression">if</error> (1 > 2) 1
}
fun blockReturnValueTypeMatch3() : Int {
    return if (1 > 2) else 1
}
fun blockReturnValueTypeMatch4() : Int {
  if (1 > 2)
    return 1.0
  else return 2.0
}
fun blockReturnValueTypeMatch5() : Int {
  if (1 > 2)
    return 1.0
  return 2.0
}
fun blockReturnValueTypeMatch6() : Int {
  if (1 > 2)
    else return 1.0
  return 2.0
}
fun blockReturnValueTypeMatch7() : Int {
  if (1 > 2)
    1.0
  else 2.0
}
fun blockReturnValueTypeMatch8() : Int {
  if (1 > 2)
    1.0
  else 2.0
  return 1
}
fun blockReturnValueTypeMatch9() : Int {
  if (1 > 2)
    1.0
}
fun blockReturnValueTypeMatch10() : Int {
    return <error descr="[INVALID_IF_AS_EXPRESSION] 'if' must have both main and 'else' branches if used as an expression">if</error> (1 > 2) 1
}
fun blockReturnValueTypeMatch11() : Int {
  if (1 > 2)
  else 1.0
}
fun blockReturnValueTypeMatch12() : Int {
  if (1 > 2)
    return 1
  else return 1.0
}
fun blockNoReturnIfValDeclaration(): Int {
  val x = 1
}
fun blockNoReturnIfEmptyIf(): Int {
  if (1 < 2) {} else {}
}
fun blockNoReturnIfUnitInOneBranch(): Int {
  if (1 < 2) {
    return 1
  } else {
    if (3 < 4) {
    } else {
      return 2
    }
  }
}
fun nonBlockReturnIfEmptyIf(): Int = if (1 < 2) {} else {}
fun nonBlockNoReturnIfUnitInOneBranch(): Int = if (1 < 2) {} else 2

val a = return 1

class A() {
}
fun illegalConstantBody(): Int = "s"
fun illegalConstantBlock(): String {
    return 1
}
fun illegalIfBody(): Int =
    if (1 < 2) 'a' else { 1.0 }
fun illegalIfBlock(): Boolean {
    if (1 < 2)
        return false
    else { return 1 }
}
fun illegalReturnIf(): Char {
    return if (1 < 2) 'a' else { 1 }
}

fun returnNothing(): Nothing {
    throw 1
}
fun f(): Int {
    if (1 < 2) { return 1 } else returnNothing()
}

fun f1(): Int = if (1 < 2) 1 else returnNothing()
