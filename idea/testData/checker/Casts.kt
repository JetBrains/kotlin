fun test() : Unit {
  var x : Int? = 0
  var y : Int = 0

  x : Int?
  y : Int
  x as Int : Int
  y <warning textAttributesKey="NOT_USED_ELEMENT_ATTRIBUTES">as Int</warning> : Int
  x <warning textAttributesKey="NOT_USED_ELEMENT_ATTRIBUTES">as Int?</warning> : Int?
  y <warning textAttributesKey="NOT_USED_ELEMENT_ATTRIBUTES">as Int?</warning> : Int?
  x as? Int : Int?
  y <warning textAttributesKey="NOT_USED_ELEMENT_ATTRIBUTES">as? Int</warning> : Int?
  x <warning textAttributesKey="NOT_USED_ELEMENT_ATTRIBUTES">as? Int?</warning> : Int?
  y <warning textAttributesKey="NOT_USED_ELEMENT_ATTRIBUTES">as? Int?</warning> : Int?
  Unit
}
