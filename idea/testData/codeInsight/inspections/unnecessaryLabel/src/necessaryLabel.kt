fun necessaryLabel() {
    @outer while (true) {
        while (false) {
            break@outer
        }
    }
}

class NecessaryLabel1() {
  fun bar() {
    val o = object {
        val s = this@NecessaryLabel1
    }
  }
}

class NecessaryLabel2() {
    inner class Inner() {
        fun f(): Any {
            return this@NecessaryLabel2
        }
    }
}
