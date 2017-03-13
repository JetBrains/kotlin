// WITH_RUNTIME

class S(val a: String, val b: String) {
  operator fun component1() : String = a
  operator fun component2() : String = b
}

operator fun S.component3() = ((a + b) as String).substring(2)

class Tester() {
  fun box() : String {
    val (o,k,ok,ok2) = S("O","K")
    return o + k + ok + ok2
  }

  operator fun S.component4() = ((a + b) as String).substring(2)
}

fun box() = Tester().box()
