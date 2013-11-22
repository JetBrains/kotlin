fun none() {}

fun unitEmptyInfer() {}
fun unitEmpty() : Unit {}
fun unitEmptyReturn() : Unit {return}
fun unitIntReturn() : Unit {return <error>1</error>}
fun unitUnitReturn() : Unit {return Unit.VALUE}
fun test1() : Any = { <error>return</error> }
fun test2() : Any = @a {<error>return@a 1</error>}
fun test3() : Any { <error>return</error> }

fun bbb() {
  return <error>1</error>
}

fun foo(<warning>expr</warning>: StringBuilder): Int {
  val c = 'a'
  when(c) {
    0.toChar() -> throw Exception("zero")
    else -> throw Exception("nonzero" + c)
  }
}


fun unitShort() : Unit = Unit.VALUE
fun unitShortConv() : Unit = <error>1</error>
fun unitShortNull() : Unit = <error>null</error>

fun intEmpty() : Int {<error>}</error>
fun intShortInfer() = 1
fun intShort() : Int = 1
//fun intBlockInfer()  {1}
fun intBlock() : Int {return 1}
fun intBlock1() : Int {<warning>1</warning><error>}</error>

fun intString(): Int = <error>"s"</error>
fun intFunctionLiteral(): Int = <error>{ 10 }</error>

fun blockReturnUnitMismatch() : Int {<error>return</error>}
fun blockReturnValueTypeMismatch() : Int {return <error>3.4</error>}
fun blockReturnValueTypeMatch() : Int {return 1}
fun blockReturnValueTypeMismatchUnit() : Int {return <error>Unit.VALUE</error>}

fun blockAndAndMismatch() : Int {
  true && false
<error>}</error>
fun blockAndAndMismatch1() : Int {
  return <error>true && false</error>
}
fun blockAndAndMismatch2() : Int {
  <warning>(return <error>true</error>) && (return <error>false</error>)</warning>
}

fun blockAndAndMismatch3() : Int {
  true || false
<error>}</error>
fun blockAndAndMismatch4() : Int {
  return <error>true || false</error>
}
fun blockAndAndMismatch5() : Int {
  <warning>(return <error>true</error>) || (return <error>false</error>)</warning>
}
fun blockReturnValueTypeMatch1() : Int {
  return if (1 > 2) <error>1.0</error> else <error>2.0</error>
}
fun blockReturnValueTypeMatch2() : Int {
  return <error>if (1 > 2) 1</error>
}
fun blockReturnValueTypeMatch3() : Int {
  return <error>if (1 > 2) else 1</error>
}
fun blockReturnValueTypeMatch4() : Int {
  if (1 > 2)
    return <error>1.0</error>
  else return <error>2.0</error>
}
fun blockReturnValueTypeMatch5() : Int {
  if (1 > 2)
    return <error>1.0</error>
  return <error>2.0</error>
}
fun blockReturnValueTypeMatch6() : Int {
  if (1 > 2)
    else return <error>1.0</error>
  return <error>2.0</error>
}
fun blockReturnValueTypeMatch7() : Int {
  if (1 > 2)
    1.0
  else 2.0
<error>}</error>
fun blockReturnValueTypeMatch8() : Int {
  if (1 > 2)
    1.0
  else 2.0
  return 1
}
fun blockReturnValueTypeMatch9() : Int {
  if (1 > 2)
    1.0
<error>}</error>
fun blockReturnValueTypeMatch10() : Int {
  return <error>if (1 > 2)
    1</error>
}
fun blockReturnValueTypeMatch11() : Int {
  if (1 > 2)
  else 1.0
<error>}</error>
fun blockReturnValueTypeMatch12() : Int {
  if (1 > 2)
    return 1
  else return <error>1.0</error>
}
fun blockNoReturnIfValDeclaration(): Int {
  val <warning>x</warning> = 1
<error>}</error>
fun blockNoReturnIfEmptyIf(): Int {
  if (1 < 2) {} else {}
<error>}</error>
fun blockNoReturnIfUnitInOneBranch(): Int {
  if (1 < 2) {
    return 1
  } else {
    if (3 < 4) {
    } else {
      return 2
    }
  }
<error>}</error>
fun nonBlockReturnIfEmptyIf(): Int = if (1 < 2) <error>{}</error> else <error>{}</error>
fun nonBlockNoReturnIfUnitInOneBranch(): Int = if (1 < 2) <error>{}</error> else 2

val a = <error>return 1</error>

class A() {
}
fun illegalConstantBody(): Int = <error>"s"</error>
fun illegalConstantBlock(): String {
    return <error>1</error>
}
fun illegalIfBody(): Int =
    if (1 < 2) <error>'a'</error> else { <error>1.0</error> }
fun illegalIfBlock(): Boolean {
    if (1 < 2)
        return false
    else { return <error>1</error> }
}
fun illegalReturnIf(): Char {
    return if (1 < 2) 'a' else { <error>1</error> }
}

fun returnNothing(): Nothing {
    throw <error>1</error>
}
fun f(): Int {
    if (1 < 2) { return 1 } else returnNothing()
}

fun f1(): Int = if (1 < 2) 1 else returnNothing()
