namespace big.delight.in.every.byte

import kotlin.io.*

fun main(args : Array<String>) {
//    for (b : Byte in Byte.MIN_VALUE..Byte.MAX_VALUE) {
//        // Problematic code does not compile
//        if (b == 0x90 .toByte())
//            println("joy")
//    }
    for (b : Byte in Byte.MIN_VALUE to Byte.MAX_VALUE) {
        // Problematic code does not compile
        if (b == 0x90 .toByte())
            println("joy")
    }
}

trait IntCompatible<T> {
    fun valueOf(i : Int) : T
    fun next(current : T) : T
    fun compareToInt(t : T, int : Int) : Int
}

class NumberRange<T : Comparable<T>>(val from : Int, val to : Int) : Range<Int>, Iterable<T>
  where class object T : IntCompatible<T> {

    override fun contains(item: Int) : Boolean = from <= item && item <= to
    fun contains(item: T) : Boolean = T.compareToInt(item, from) >= 0 && T.compareToInt(item, to) <= 0

    override fun iterator(): Iterator<T> = object : Iterator<T> {
        private var current : T = T.valueOf(from)
        override fun next() : T {
            if (!hasNext) throw java.util.NoSuchElementException()
            val result = current
            current = T.next(current)
            return result
        }
        override val hasNext : Boolean
          get() = T.compareToInt(current, to) < 0
    }
}

fun Byte.to(to : Byte) = NumberRange<Byte>(this.toInt(), to.toInt())