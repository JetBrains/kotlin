// COMPILER_ARGUMENTS: -XXLanguage:-NewInference
// RUNTIME

fun none() {}

fun unitEmptyInfer() {}
fun unitEmpty() : Unit {}
fun unitEmptyReturn() : Unit {return}
fun unitIntReturn() : Unit {return <error descr="[RETURN_TYPE_MISMATCH] Return type mismatch: expected kotlin/Unit, actual kotlin/Int">1</error>}
fun unitUnitReturn() : Unit {return Unit}
fun test1() : Any = { <error descr="[RETURN_NOT_ALLOWED] 'return' is not allowed here">return</error> }
fun test2() : Any = a@ {return@a 1}
fun test3() : Any { return }

fun bbb() {
  return <error descr="[RETURN_TYPE_MISMATCH] Return type mismatch: expected kotlin/Unit, actual kotlin/Int">1</error>
}

fun foo(expr: StringBuilder): Int {
  val c = 'a'
  when(c) {
    0.toChar() -> throw Exception("zero")
    else -> throw Exception("nonzero" + c)
  }
}


fun unitShort() : Unit = Unit
fun unitShortConv() : Unit = <error descr="[RETURN_TYPE_MISMATCH] Return type mismatch: expected kotlin/Unit, actual kotlin/Int">1</error>
fun unitShortNull() : Unit = <error descr="[NULL_FOR_NONNULL_TYPE] ">null</error>

fun intEmpty() : Int {}
fun intShortInfer() = 1
fun intShort() : Int = 1
//fun intBlockInfer()  {1}
fun intBlock() : Int {return 1}
fun intBlock1() : Int {1}

fun intString(): Int = <error descr="[RETURN_TYPE_MISMATCH] Return type mismatch: expected kotlin/Int, actual kotlin/String">"s"</error>
fun intFunctionLiteral(): Int = <error descr="[RETURN_TYPE_MISMATCH] Return type mismatch: expected kotlin/Int, actual kotlin/Function0<kotlin/Int>">{ 10 }</error>

fun blockReturnUnitMismatch() : Int {<error descr="[RETURN_TYPE_MISMATCH] Return type mismatch: expected kotlin/Int, actual kotlin/Unit">return</error>}
fun blockReturnValueTypeMismatch() : Int {return <error descr="[RETURN_TYPE_MISMATCH] Return type mismatch: expected kotlin/Int, actual kotlin/Double">3.4</error>}
fun blockReturnValueTypeMatch() : Int {return 1}
fun blockReturnValueTypeMismatchUnit() : Int {return <error descr="[RETURN_TYPE_MISMATCH] Return type mismatch: expected kotlin/Int, actual kotlin/Unit">Unit</error>}

fun blockAndAndMismatch() : Int {
  true && false
}
fun blockAndAndMismatch1() : Int {
  return <error descr="[RETURN_TYPE_MISMATCH] Return type mismatch: expected kotlin/Int, actual kotlin/Boolean">true && false</error>
}
fun blockAndAndMismatch2() : Int {
  (return <error descr="[RETURN_TYPE_MISMATCH] Return type mismatch: expected kotlin/Int, actual kotlin/Boolean">true</error>) && (return <error descr="[RETURN_TYPE_MISMATCH] Return type mismatch: expected kotlin/Int, actual kotlin/Boolean">false</error>)
}

fun blockAndAndMismatch3() : Int {
  true || false
}
fun blockAndAndMismatch4() : Int {
  return <error descr="[RETURN_TYPE_MISMATCH] Return type mismatch: expected kotlin/Int, actual kotlin/Boolean">true || false</error>
}
fun blockAndAndMismatch5() : Int {
  (return <error descr="[RETURN_TYPE_MISMATCH] Return type mismatch: expected kotlin/Int, actual kotlin/Boolean">true</error>) || (return <error descr="[RETURN_TYPE_MISMATCH] Return type mismatch: expected kotlin/Int, actual kotlin/Boolean">false</error>)
}
fun blockReturnValueTypeMatch1() : Int {
  return <error descr="[RETURN_TYPE_MISMATCH] Return type mismatch: expected kotlin/Int, actual kotlin/Double">if (1 > 2) 1.0 else 2.0</error>
}
fun blockReturnValueTypeMatch2() : Int {
    return <error descr="[INVALID_IF_AS_EXPRESSION] 'if' must have both main and 'else' branches if used as an expression">if</error> (1 > 2) 1
}
fun blockReturnValueTypeMatch3() : Int {
    return <error descr="[RETURN_TYPE_MISMATCH] Return type mismatch: expected kotlin/Int, actual kotlin/Any">if (1 > 2) else 1</error>
}
fun blockReturnValueTypeMatch4() : Int {
  if (1 > 2)
    return <error descr="[RETURN_TYPE_MISMATCH] Return type mismatch: expected kotlin/Int, actual kotlin/Double">1.0</error>
  else return <error descr="[RETURN_TYPE_MISMATCH] Return type mismatch: expected kotlin/Int, actual kotlin/Double">2.0</error>
}
fun blockReturnValueTypeMatch5() : Int {
  if (1 > 2)
    return <error descr="[RETURN_TYPE_MISMATCH] Return type mismatch: expected kotlin/Int, actual kotlin/Double">1.0</error>
  return <error descr="[RETURN_TYPE_MISMATCH] Return type mismatch: expected kotlin/Int, actual kotlin/Double">2.0</error>
}
fun blockReturnValueTypeMatch6() : Int {
  if (1 > 2)
    else return <error descr="[RETURN_TYPE_MISMATCH] Return type mismatch: expected kotlin/Int, actual kotlin/Double">1.0</error>
  return <error descr="[RETURN_TYPE_MISMATCH] Return type mismatch: expected kotlin/Int, actual kotlin/Double">2.0</error>
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
  else return <error descr="[RETURN_TYPE_MISMATCH] Return type mismatch: expected kotlin/Int, actual kotlin/Double">1.0</error>
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
fun nonBlockReturnIfEmptyIf(): Int = <error descr="[RETURN_TYPE_MISMATCH] Return type mismatch: expected kotlin/Int, actual kotlin/Unit">if (1 < 2) {} else {}</error>
fun nonBlockNoReturnIfUnitInOneBranch(): Int = <error descr="[RETURN_TYPE_MISMATCH] Return type mismatch: expected kotlin/Int, actual kotlin/Any">if (1 < 2) {} else 2</error>

val a = <error descr="[RETURN_NOT_ALLOWED] 'return' is not allowed here">return</error> 1

class A() {
}
fun illegalConstantBody(): Int = <error descr="[RETURN_TYPE_MISMATCH] Return type mismatch: expected kotlin/Int, actual kotlin/String">"s"</error>
fun illegalConstantBlock(): String {
    return <error descr="[RETURN_TYPE_MISMATCH] Return type mismatch: expected kotlin/String, actual kotlin/Int">1</error>
}
fun illegalIfBody(): Int =
    <error descr="[RETURN_TYPE_MISMATCH] Return type mismatch: expected kotlin/Int, actual it(kotlin/Comparable<*> & java/io/Serializable)">if (1 < 2) 'a' else { 1.0 }</error>
fun illegalIfBlock(): Boolean {
    if (1 < 2)
        return false
    else { return <error descr="[RETURN_TYPE_MISMATCH] Return type mismatch: expected kotlin/Boolean, actual kotlin/Int">1</error> }
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
