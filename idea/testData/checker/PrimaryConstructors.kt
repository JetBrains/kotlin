class X {
  <error>val x : Int</error>
}

open class Y() {
  val x : Int = 2
}

class Y1 {
  val x : Int get() = 1
}

class Z : Y() {
}
// FIR_COMPARISON