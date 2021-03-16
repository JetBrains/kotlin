// FIR_IDENTICAL

fun <T> checkSubtype(t: T) = t

fun test() : Unit {
  var x : Int? = 0
  var y : Int = 0

  checkSubtype<Int?>(x)
  checkSubtype<Int>(y)
  checkSubtype<Int>(x as Int)
  checkSubtype<Int>(y <warning descr="[USELESS_CAST] No cast needed">as Int</warning>)
  checkSubtype<Int?>(x as Int?)
  checkSubtype<Int?>(y as Int?)
  checkSubtype<Int?>(x as Int?)
  checkSubtype<Int?>(y as? Int)
  checkSubtype<Int?>(x as? Int?)
  checkSubtype<Int?>(y as? Int?)
  Unit
}
