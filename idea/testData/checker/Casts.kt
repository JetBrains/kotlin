fun test() : Unit {
  var x : Int? = 0
  var y : Int = 0

  x : Int?
  y : Int
  x as Int : Int
  y <warning>as</warning> Int : Int
  x <warning>as</warning> Int? : Int?
  y <warning>as</warning> Int? : Int?
  x as? Int : Int?
  y <warning>as?</warning> Int : Int?
  x <warning>as?</warning> Int? : Int?
  y <warning>as?</warning> Int? : Int?
  Unit
}
