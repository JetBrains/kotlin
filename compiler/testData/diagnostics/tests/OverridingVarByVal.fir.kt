open class Var() {
  open var v : Int = 1
}

interface VarT {
  var v : Int
}

class Val() : Var(), VarT {
  override val v : Int = 1
}

class Var2() : Var() {
  override var v : Int = 1
}
