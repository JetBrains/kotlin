// WITH_STDLIB

fun findPairless(a : IntArray) : Int {
  loop@ for (i in a.indices) {
    for (j in a.indices) {
      if (i != j && a[i] == a[j]) continue@loop
    }
    return a[i]
  }
  return -1
}

fun hasDuplicates(a : IntArray) : Boolean {
  var duplicate = false
  loop@ for (i in a.indices) {
    for (j in a.indices) {
      if (i != j && a[i] == a[j]) {
        duplicate = true
        break@loop
      }
    }
  }
  return duplicate
}

fun box() : String {
    val a = IntArray(5)
    a[0] = 0
    a[1] = 0
    a[2] = 1
    a[3] = 1
    a[4] = 5
    if(findPairless(a) != 5) return "fail"
    return if(hasDuplicates(a))  "OK" else "fail"

}
