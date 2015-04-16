fun test() : Unit {
  var x : Int? = 0
  var y : Int = 0

  x : Int?
  y : Int
  x as Int : Int
  y <warning>as Int</warning> : Int
  x <warning>as Int?</warning> : Int?
  y <warning>as Int?</warning> : Int?
  x <warning>as Int?</warning> : Int?
  y <warning>as? Int</warning> : Int?
  x <warning>as? Int?</warning> : Int?
  y <warning>as? Int?</warning> : Int?
  Unit
}
