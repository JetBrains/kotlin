package toplevelObjectDeclarations

  open class Foo(y : Int) {
    open fun foo() : Int = 1
  }

  <!NO_VALUE_FOR_PARAMETER!>class T : <!SUPERTYPE_NOT_INITIALIZED!>Foo<!> {}<!>

  <!NO_VALUE_FOR_PARAMETER{LT}!>object A : <!SUPERTYPE_NOT_INITIALIZED!>Foo<!> {
    val x : Int = 2

    fun test() : Int {
      return x + foo(<!NO_VALUE_FOR_PARAMETER{PSI}!>)<!>
    }
  }<!>

  object B : A {}

  val x = A.foo()

  val y = object : Foo(x) {
    init {
      x + 12
    }

    override fun foo() : Int = 1
  }

  val z = y.foo()
