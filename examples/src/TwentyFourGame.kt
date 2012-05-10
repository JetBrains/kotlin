package TwentyFourGame

/**
 * @author yole
 */
import java.util.*
import java.io.*

fun StringBuilder.takeFirst(): Char {
  if (this.length() == 0) return 0.toChar()
  val c = this.charAt(0)
  this.deleteCharAt(0)
  return c
}

class Evaluator(val expr: StringBuilder, val numbers: ArrayList<Int>) {
  fun checkFirst(expect: Char): Boolean {
    if (expr.length() > 0 && expr.charAt(0) == expect) {
      expr.deleteCharAt(0)
      return true
    }
    return false
  }

  fun evaluateArg(): Int {
    val c = expr.takeFirst()
    when(c) {
      in '0'..'9' -> {
        val n = c - '0'
        val index = numbers.indexOf(n)
        if (index < 0) throw Exception("You used incorrect number: " + n)
        numbers.remove(index) // gotcha: conflict between remove(Object) and remove(int)
        return n
      }
      '(' -> {
        val result = evaluate()
        if (expr.takeFirst() != ')') throw Exception(") expected")
        return result
      }
      0.toChar() -> throw Exception("Syntax error: Character expected")
      else -> throw Exception("Syntax error: Unrecognized character " + c)
    }
  }

  fun evaluateMult(): Int {
    val lhs = evaluateArg()
    if (checkFirst('*'))
      return lhs * evaluateMult()
    if (checkFirst('/'))
      return lhs / evaluateMult()
    return lhs
  }

  fun evaluate(): Int {
    val lhs = evaluateMult()
    if (checkFirst('+'))
      return lhs + evaluate()
    if (checkFirst('-'))
      return lhs - evaluate()
    return lhs
  }

  fun evaluateAll(): Int {
    val result = evaluate()
    if (expr.length() > 0) throw Exception("unexpected text: " + expr)
    return result
  }
}

fun main(args: Array<String>) {
  println("24 game")
  val numbers = ArrayList<Int>(4)
  val rnd = Random();
  val prompt = StringBuilder()
  for(val i in 0..3) {
    val n = rnd.nextInt(9) + 1
    numbers.add(n)
    if (i > 0) prompt.append(" ");
    prompt.append(n)
  }
  println("Your numbers: " + prompt)
  println("Enter your expression:")
  val reader = BufferedReader(InputStreamReader(System.`in`))
  val expr = StringBuilder(reader.readLine())
  try {
    val result = Evaluator(expr, numbers).evaluateAll()
    if (result != 24)
      println("Sorry, that's " + result)
    else
      println("You won!");
  }
  catch(e: Throwable) {
    println(e.getMessage())
  }
}