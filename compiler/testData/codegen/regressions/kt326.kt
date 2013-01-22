package test

class List<T>(len: Int, cls : java.lang.Class) {
  val a : Array<T?> = Array<T?>(len)

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
  println(d.a[0])

  val a = List<String>(1)
  a.a[0] = "1"
  println(a.a[0])

  val b = List<Int?>(1)
  b.a[0] = 10
  println(b.a[0])

  val c = List<Array<Int>>(1)
  c.a[0] = Array<Int>(4,{-1})
  println(c.a[0]?.size)

  val e = List<Int>(5)
  e.a[0] = 0
  e.a[1] = 1
  e.a[2] = 2
  e.a[3] = 3
  e.a[4] = 4
  e.reverse()
  for(el in e.a)
    println(el)

  return "OK"
}

fun println(s : Any?) {
  System.out?.println(s);
}
