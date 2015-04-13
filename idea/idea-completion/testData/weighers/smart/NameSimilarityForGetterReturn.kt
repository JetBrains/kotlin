class C(val x: X) {
    val foo: String
      get() {
          return x.<caret>
      }
}

class X {
    val s = ""
    fun getFoo() = ""
}

// ORDER: getFoo, s, toString
