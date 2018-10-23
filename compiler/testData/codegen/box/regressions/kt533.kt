// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME
// FULL_JDK

package mask

import java.io.*
import java.util.*

fun box() : String {
    val input = StringReader("/aaa/bbb/ccc/ddd")

    val luhny = Luhny()
    input.forEachChar {
        luhny.charIn(it)
    }
    luhny.printAll()
    return "OK"

}

class Luhny() {
    val buffer = LinkedList<Char>()
    val digits = LinkedList<Int>()

    var toBeMasked = 0

    fun charIn(it : Char) {
        buffer.push(it)

        // Commented for KT-621
        // when (it) {
        //   .isDigit() => digits.push(it.toInt() - '0'.toInt())
        //   ' ', '-' => {}
        //   else => {
        //           printAll()
        //           digits.clear()
        //   }
        // }

        if (it.isDigit()) {
            digits.push(it - '0')
        } else if (it == ' ' || it == '-') {
        } else {
            printAll()
            digits.clear()
        }

        if (digits.size > 16)
          printOneDigit()
        check()
    }

    fun check() {
        if (digits.size < 14) return
        val sum = digits.sum { i, d ->
            if (i % 2 != 0)
                d * 2 / 10 +  d * 2 % 10
            else d
        }
        if (sum % 10 == 0) toBeMasked = digits.size
    }

    fun printOneDigit() {
        while (!buffer.isEmpty()) {
            val c = buffer.pop()
            out(c)
            if (c.isDigit()) {
                digits.pop()
                return
            }
        }
    }

    fun printAll() {
        while (!buffer.isEmpty())
          out(buffer.pop())
    }

    fun out(c : Char) {
        if (toBeMasked > 0) {
            print('X')
            toBeMasked--
        }
        else {
            // print(c)
        }
    }
}

fun LinkedList<Int>.sum(f : (Int, Int) -> Int) : Int {
    var sum = 0
    var i = 0
    for (d in backwards()) {
        sum += f(i, d)
        i++
    }
    return sum
}

fun <T> List<T>.backwards() : Iterable<T> = object : Iterable<T> {
  override fun iterator() : Iterator<T> =
      object : Iterator<T> {
          var current = size
          override fun next() : T = get(--current)
          override fun hasNext() : Boolean = current > 0
      }
}

// fun Char.isDigit() = Character.isDigit(this)

//class Queue<T>(initialBufSize : Int) {
//
//    private var bufSize = initialBufSize
//    private val buf = Array<T>(initialBufSize)
//    private var head = 0
//    private var tail = 0
//    private var size = 0
//
//    val empty : Boolean get() = size == 0
//
//    private fun prev(i : Int) = (bufSize + i - 1) % bufSize
//    private fun next(i : Int) = (i + 1) % bufSize
//
//    fun push(c : T) {
//        buf[tail] = c
//        tail = prev(tail)
//        size++
//    }
//
//    fun pop() : T {
//        if (size == 0) throw IllegalStateException()
//        size--
//        val result = buf[head]
//        head = prev(head)
//        return result
//    }
//
//    fun clear() {}
//}

fun Reader.forEachChar(body : (Char) -> Unit) {
    do {
        var i = read();
        if (i == -1) break
        body(i.toChar())
    } while(true)
}
