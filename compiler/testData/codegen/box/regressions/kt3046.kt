// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME

fun box(): String {
  val bool = true
  if (bool.javaClass != Boolean::class.java) return "javaClass function on boolean fails"
  val b = 1.toByte()
  if (b.javaClass != Byte::class.java) return "javaClass function on byte fails"
  val s = 1.toShort()
  if (s.javaClass != Short::class.java) return "javaClass function on short fails"
  val c = 'c'
  if (c.javaClass != Char::class.java) return "javaClass function on char fails"
  val i = 1
  if (i.javaClass != Int::class.java) return "javaClass function on int fails"
  val l = 1.toLong()
  if (l.javaClass != Long::class.java) return "javaClass function on long fails"
  val f = 1.toFloat()
  if (f.javaClass != Float::class.java) return "javaClass function on float fails"
  val d = 1.0
  if (d.javaClass != Double::class.java) return "javaClass function on double fails"

  return "OK"
}
