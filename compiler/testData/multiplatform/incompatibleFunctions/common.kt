platform infix fun Int.plus(s: CharSequence): Int
platform fun Int.minus(s: String): Int

platform operator fun Double.times(x: CharArray)
platform fun Double.divide(x: ByteArray)

platform external fun f1()
platform fun g1()

platform inline fun f2()
platform fun g2()

platform tailrec fun f3()
platform fun g3()
