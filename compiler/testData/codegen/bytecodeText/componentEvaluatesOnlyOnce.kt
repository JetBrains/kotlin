class S(val a: String, val b: String) {
  fun component1() : String = a
  fun component2() : String = b
}

fun S.component3() = ((a + b) as java.lang.String).substring(2)

class Tester() {
  fun box() : String {
    val (o,k,ok,ok2) = S("O","K")
    return o + k + ok + ok2
  }

  fun S.component4() = ((a + b) as java.lang.String).substring(2)
}

fun box() = Tester().box()

// 1 NEW S
