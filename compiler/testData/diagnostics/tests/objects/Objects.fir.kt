package toplevelObjectDeclarations

  open class Foo(y : Int) {
    open fun foo() : Int = 1
  }

  <!INAPPLICABLE_CANDIDATE!>class T : Foo {}<!>

  <!INAPPLICABLE_CANDIDATE!>object A : Foo {
    val x : Int = 2

    fun test() : Int {
      return x + foo()
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