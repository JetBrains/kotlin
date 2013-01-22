class List<T>(len: Int) {
  val a = Array<T?>(len) { null }

  fun reverse() {
    var i = 0
    var j = a.size-1
    while(i < j) {
        val x  = a[i]
        a[i++] = a[j]
        a[j--] = x
    }
  }
}

fun box() : String {
  val d = List<Int>(1)
  d.a[0] = 10
  checkEquals(d.a[0], 10)

  val a = List<String>(1)
  a.a[0] = "1"
  checkEquals(a.a[0], "1")

  val b = List<Int?>(1)
  b.a[0] = 10
  checkEquals(b.a[0], 10)

  val c = List<Array<Int>>(1)
  c.a[0] = Array<Int>(4,{-1})
  checkEquals(c.a[0]?.size, 4)

  val e = List<Int>(5)
  e.a[0] = 0
  e.a[1] = 1
  e.a[2] = 2
  e.a[3] = 3
  e.a[4] = 4
  e.reverse()
  for (i in 0..4)
    checkEquals(e.a[i], 4-i)

  return "OK"
}

fun checkEquals(a: Any?, b: Any?) {
  if (a != b) throw AssertionError("Expected: $b, actual: $a")
}

fun main(args: Array<String>) {
  println(box())
}
