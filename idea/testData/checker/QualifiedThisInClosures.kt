    class A(val a:Int) {

      inner class B() {
        val x = this@B : B
        val y = this@A : A
        val z = this : B
        val Int.xx : Int get() = this : Int
        fun Char.xx() : Double.() -> Unit {
          this : Char
          val <warning>a</warning>: Double.() -> Unit = { this : Double + this@xx : Char}
          val <warning>b</warning>: Double.() -> Unit = a@{this@a : Double + this@xx : Char}
          val <warning>c</warning> = a@{<error>this@a</error> <error>+</error> this@xx : Char}
          return (a@{this@a : Double + this@xx : Char})
        }
      }
    }
