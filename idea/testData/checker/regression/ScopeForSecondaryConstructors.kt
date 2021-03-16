// FIR_IDENTICAL

fun <T> checkSubtype(t: T) = t

class Foo(var bar : Int, var barr : Int, var barrr : Int) {
  init {
    bar = 1
    barr = 1
    barrr = 1
    checkSubtype<Int>(1)
    checkSubtype<Foo>(this)
  }

  init {
    bar = 1
    this.bar
    checkSubtype<Int>(1)
    val <warning>a</warning> : Int =1
    checkSubtype<Foo>(this)
  }
}
