// !CHECK_TYPE

fun test() : Unit {
  var x : Int? = 0
  var y : Int = 0

  checkSubtype<Int?>(x)
  checkSubtype<Int>(y)
  checkSubtype<Int>(x as Int)
  checkSubtype<Int>(y <!USELESS_CAST!>as Int<!>)
  checkSubtype<Int?>(x <!USELESS_CAST!>as Int?<!>)
  checkSubtype<Int?>(y <!USELESS_CAST!>as Int?<!>)
  checkSubtype<Int?>(x <!USELESS_CAST!>as? Int<!>)
  checkSubtype<Int?>(y <!USELESS_CAST!>as? Int<!>)
  checkSubtype<Int?>(x <!USELESS_CAST!>as? Int?<!>)
  checkSubtype<Int?>(y <!USELESS_CAST!>as? Int?<!>)
  Unit
}
