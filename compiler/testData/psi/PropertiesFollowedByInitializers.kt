class Foo() {
  val a : Int get = 1
  var b : Int get() = 1; set
  var b1 : Int get() = 1; set {1}
  val b2 : Int get
  {

  }
  val b3 : Int get {
    return 1
  }
  val b4 : Int get; {
  }

  var b5 : Int get abstract set
  var b6 : Int get [a] abstract set
  var b7 : Int get [a] abstract {}
}

class PublicVar() { public var foo = 0; }
class PublicVar() { public var foo = 0; var x : Int }
class PublicVar() { public var foo = 0 }
class PublicVar() { public var foo get set }
class PublicVar() { public var foo get set }

val now: Long get() = System.currentTimeMillis(); fun foo() = now
