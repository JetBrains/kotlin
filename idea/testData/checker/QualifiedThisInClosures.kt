fun <T> checkSubtype(t: T) = t

class A(val a:Int) {

      inner class B() {
        val x = checkSubtype<B>(this@B)
        val y = checkSubtype<A>(this@A)
        val z = checkSubtype<B>(this)
        val Int.xx : Int get() = checkSubtype<Int>(this)
        fun Char.xx() : Double.() -> Unit {
          checkSubtype<Char>(this)
          val <warning>a</warning>: Double.() -> Unit = { checkSubtype<Double>(this) + checkSubtype<Char>(this@xx) }
          val <warning>b</warning>: Double.() -> Unit = a@{checkSubtype<Double>(this@a) + checkSubtype<Char>(this@xx) }
          val <warning>c</warning> = a@{<error>this@a</error> <error>+</error> checkSubtype<Char>(this@xx) }
          return (a@{checkSubtype<Double>(this@a) + checkSubtype<Char>(this@xx) })
        }
      }
    }
