package toplevelObjectDeclarations

  open class Foo(y : Int) {
    open fun foo() : Int = 1
  }

  class T : <!SUPERTYPE_NOT_INITIALIZED!>Foo<!> {}

  object A : <!SUPERTYPE_NOT_INITIALIZED!>Foo<!> {
    val x : Int = 2

    fun test() : Int {
      return x + foo()
    }
  }

  object B : <!UNRESOLVED_REFERENCE!>A<!> {}

  val x = A.foo()

  val y = object : Foo(x) {
    {
      x + 12
    }

    override fun foo() : Int = 1
  }

  val z = y.foo()
