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
        luhny.process(it)
    }
    luhny.printAll()
    return "OK"
}

class Luhny() {
    private val buffer = ArrayDeque<Char>()
    private val digits = ArrayDeque<Int>(16)

    private var toBeMasked = 0

    fun process(it : Char) {
        buffer.addLast(it)

        // Commented for KT-621
        // when (it) {
        //     .isDigit() => digits.addLast(it.toInt() - '0'.toInt())
        //     ' ', '-'   => {}
        //     else       => printAll()
        // }

        if (it.isDigit()) {
            digits.addLast(it - '0')
        } else if (it == ' ' || it == '-') {
        } else {
            printAll()
        }

        if (digits.size > 16)
          printOneDigit()
        check()
    }

    private fun check() {
        val size = digits.size
        if (size < 14) return
        val sum = digits.sum {i, d ->
            if (i % 2 == size % 2) double(d) else d
        }
//        var sum = 0
//        var i = 0
//        for (d in digits) {
//            sum += if (i % 2 == size % 2) double(d) else d
//            i++
//        }
        if (sum % 10 == 0) toBeMasked = digits.size
    }

    private fun double(d : Int) = d * 2 / 10 + d * 2 % 10

    private fun printOneDigit() {
        while (!buffer.isEmpty()) {
            val c = buffer.removeFirst()!!
            print(c)
            if (c.isDigit()) {
                digits.removeFirst()
                return
            }
        }
    }

    fun printAll() {
        while (!buffer.isEmpty())
          print(buffer.removeFirst())
        digits.clear()
    }

    private fun print(c : Char) {
        if (c.isDigit() && toBeMasked > 0) {
            kotlin.io.print('X')
            toBeMasked--
        } else {
            // kotlin.io.print(c)
        }
    }
}

// fun Char.isDigit() = Character.isDigit(this)

fun Iterable<Int>.sum(f : (index : Int, value : Int) -> Int) : Int {
    var sum = 0
    var i = 0
    for (d in this) {
        sum += f(i, d)
        i++
    }
    return sum
}

fun Reader.forEachChar(body : (Char) -> Unit) {
    do {
        var i = read();
        if (i == -1) break
        body(i.toChar())
    } while(true)
}
