// IGNORE_FIR

fun test() {
  <expr>A.Companion.f</expr>
}

class A() {
  companion object {
    private object f {

    }
  }
}