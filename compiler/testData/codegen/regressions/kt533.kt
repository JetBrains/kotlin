namespace mask

import std.io.*
import java.io.*
import java.util.*

fun box() : String {
    val input = StringReader("/Users/abreslav/work/jet/docs/luhnybin/src/test")

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
        when (it) {
            .isDigit() => digits.push(it.int - '0'.int)
            ' ', '-' => {}
            else => {
                    printAll()
                    digits.clear()
            }
        }
        if (digits.size() > 16)
          printOneDigit()
        check()
    }

    fun check() {
        if (digits.size() < 14) return
        val sum = digits.sum { i, d =>
            if (i % 2 != 0)
                d * 2 / 10 +  d * 2 % 10
            else d
        }
        if (sum % 10 == 0) toBeMasked = digits.size()
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
            print(c)
        }
    }
}

fun LinkedList<Int>.sum(f : fun(Int, Int) : Int) : Int {
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
          var current = size()
          override fun next() : T = get(--current)
          override val hasNext : Boolean get() = current > 0
      }
}

fun Char.isDigit() = Character.isDigit(this)

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

fun Reader.forEachChar(body : fun(Char) : Unit) {
    do {
        var i = read();
        if (i == -1) break
        body(i.chr)
    } while(true)
}