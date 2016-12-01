fun foo1() {
  var i = 0
  while (true) {
    if ((i++ % 2) == 0) continue
    println("while " + i.toString())
    if (i > 4) break
  }
}

fun foo2() {
  var i = 0
  do {
    if ((i++ % 2) == 0) continue
    println("while " + i.toString())
    if (i > 4) break
  } while (true)
}

fun foo3() {
  for (i in 1..6) {
    if ((i % 2) == 0) continue
    println("while " + i.toString())
    if (i > 4) break
  }
}

fun main(args: Array<String>) {
  foo1()
  foo2()
  foo3()
}
