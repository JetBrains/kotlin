fun box(): String {
  val bool = true
  if (bool.javaClass != javaClass<Boolean>()) return "javaClass function on boolean fails"
  val b = 1.toByte()
  if (b.javaClass != javaClass<Byte>()) return "javaClass function on byte fails"
  val s = 1.toShort()
  if (s.javaClass != javaClass<Short>()) return "javaClass function on short fails"
  val c = 'c'
  if (c.javaClass != javaClass<Char>()) return "javaClass function on char fails"
  val i = 1
  if (i.javaClass != javaClass<Int>()) return "javaClass function on int fails"
  val l = 1.toLong()
  if (l.javaClass != javaClass<Long>()) return "javaClass function on long fails"
  val f = 1.toFloat()
  if (f.javaClass != javaClass<Float>()) return "javaClass function on float fails"
  val d = 1.0
  if (d.javaClass != javaClass<Double>()) return "javaClass function on double fails"

  return "OK"
}
