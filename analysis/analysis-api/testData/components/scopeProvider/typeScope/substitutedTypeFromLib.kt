// WITH_STDLIB

fun foo() {
   val l = listOf(1, 2, 3)
   val l2 = l.map { it * 2 to it }

   val el = l2.find { it.first == 1 } ?: return
   println(<expr>el</expr>)
}