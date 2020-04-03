// RUNTIME
import java.util.*

import java.io.*

fun takeFirst(expr: StringBuilder): Char {
  val c = expr.get(0)
  expr.deleteCharAt(0)
  return c
}

fun evaluateArg(expr: CharSequence, numbers: ArrayList<Int>): Int {
  if (expr.length == 0) throw Exception("Syntax error: Character expected");
  val c = takeFirst(<error descr="[TYPE_MISMATCH] Type mismatch: inferred type is CharSequence but kotlin.text.StringBuilder /* = java.lang.StringBuilder */ was expected">expr</error>)
  if (c >= '0' && c <= '9') {
    val n = c - '0'
    if (!numbers.contains(n)) throw Exception("You used incorrect number: " + n)
    numbers.remove(n)
    return n
  }
  throw Exception("Syntax error: Unrecognized character " + c)
}

fun evaluateAdd(expr: StringBuilder, numbers: ArrayList<Int>): Int {
  val lhs = evaluateArg(expr, numbers)
  if (expr.length > 0) {

  }
  return lhs
}

fun evaluate(expr: StringBuilder, numbers: ArrayList<Int>): Int {
  val lhs = evaluateAdd(expr, numbers)
  if (expr.length > 0) {
    val <warning descr="[UNUSED_VARIABLE] Variable 'c' is never used">c</warning> = expr.get(0)
    expr.deleteCharAt(0)
  }
  return lhs
}

fun main(<warning descr="[UNUSED_PARAMETER] Parameter 'args' is never used">args</warning>: Array<String>) {
  System.out.println("24 game")
  val numbers = ArrayList<Int>(4)
  val rnd = Random();
  val prompt = StringBuilder()
  for(i in 0..3) {
    val n = rnd.nextInt(9) + 1
    numbers.add(n)
    if (i > 0) prompt.append(" ");
    prompt.append(n)
  }
  System.out.println("Your numbers: " + prompt)
  System.out.println("Enter your expression:")
  val reader = BufferedReader(InputStreamReader(System.`in`))
  val expr = StringBuilder(reader.readLine()!!)
  try {
    val result = evaluate(expr, numbers)
    if (result != 24)
      System.out.println("Sorry, that's " + result)
    else
      System.out.println("You won!");
  }
  catch(e: Throwable) {
    System.out.println(e.message)
  }
}
