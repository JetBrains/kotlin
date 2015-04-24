fun <T> checkSubtype(t: T) = t

fun test() : Unit {
  var x : Int? = 0
  var y : Int = 0

  checkSubtype<Int?>(x)
  checkSubtype<Int>(y)
  checkSubtype<Int>(x as Int)
  checkSubtype<Int>(y <warning>as Int</warning>)
  checkSubtype<Int?>(x <warning>as Int?</warning>)
  checkSubtype<Int?>(y as Int?)
  checkSubtype<Int?>(x <warning>as Int?</warning>)
  checkSubtype<Int?>(y <warning>as? Int</warning>)
  checkSubtype<Int?>(x <warning>as? Int?</warning>)
  checkSubtype<Int?>(y as? Int?)
  Unit
}
