package luhnybin

import kotlin.io.*
import java.io.*
import java.util.ArrayDeque

fun isCardChar(it : Char) : Boolean = it == ' ' || it == '-' || it.isDigit()

fun applyMask(a : String) : String {
  val luhny = Luhny()
  for (c in a) {
    luhny.process(c)
  }
  luhny.processEnd()

  return luhny.output.toString()!!
}

class Luhny() {
  private val buffer = ArrayDeque<Char>()
  private val digits = ArrayDeque<Int>(16)

  private var toBeMasked = 0
  val output = StringBuilder()

  fun process(it : Char) {
    buffer.addLast(it)

    when {
      it.isDigit() -> digits.addLast(it.toDigit())
      it == ' ' || it == '-'   -> {
      }
      else -> printAll()
    }

    if (digits.size() > 16) {
      printUntil { it.isDigit() }
      digits.removeFirst()
    }

    check()
  }

  fun processEnd() {
    while (digits.size() >= 14) {
      check()
      printUntil { it.isDigit() }
      digits.removeFirst()
    }

    printAll()
  }

  private fun check() {
    val size = digits.size()
    if (size < 14) return

    var sum = 0
    var i = 0
    for (d in digits) {
      sum += if (i % 2 == size % 2) double(d) else d
      i++
    }

    if (sum % 10 == 0)
      toBeMasked = size
  }

  private fun double(d : Int) = d * 2 / 10 + d * 2 % 10

  fun printAll() {
    printUntil { false }
    digits.clear()
  }

  private fun printUntil(f : (Char) -> Boolean) {
    while (!buffer.isEmpty()) {
      val c = buffer.removeFirst()
      printChar(c)
      if (f(c)) return
    }
  }

  private fun printChar(c : Char) {
    if (c.isDigit() && toBeMasked > 0) {
      output.append('X')
      toBeMasked--
    } else {
      output.append(c)
    }
  }
}

fun Char.isDigit() = Character.isDigit(this)
fun Char.toDigit() = this.toInt() - '0'.toInt()
