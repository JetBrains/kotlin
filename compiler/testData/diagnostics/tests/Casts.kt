fun test() : Unit {
  var x : Int? = 0
  var y : Int = 0

  x : Int?
  y : Int
  x as Int : Int
  y <!USELESS_CAST!>as<!> Int : Int
  x <!USELESS_CAST!>as<!> Int? : Int?
  y <!USELESS_CAST_STATIC_ASSERT_IS_FINE!>as<!> Int? : Int?
  x as? Int : Int?
  y <!USELESS_CAST!>as?<!> Int : Int?
  x <!USELESS_CAST!>as?<!> Int? : Int?
  y <!USELESS_CAST_STATIC_ASSERT_IS_FINE!>as?<!> Int? : Int?
  Unit
}
